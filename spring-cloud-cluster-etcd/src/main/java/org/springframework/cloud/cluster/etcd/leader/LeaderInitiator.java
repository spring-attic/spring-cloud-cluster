/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cluster.etcd.leader;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.Lifecycle;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;

/**
 * Bootstrap leadership {@link org.springframework.cloud.cluster.leader.Candidate candidates}
 * with Etcd. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Venil Noronha
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	private final static int TTL = 10;
	private final static int HEART_BEAT_SLEEP = 1000 * (TTL / 2);
	private final static String DEFAULT_NAMESPACE = "spring-cloud";
	
	/**
	 * {@link EtcdClient} instance.
	 */
	private final EtcdClient client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Etcd namespace.
	 */
	private final String namespace;
	
	/**
	 * Executor service for running leadership daemon.
	 */
	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Etcd-Leadership");
			thread.setDaemon(true);
			return thread;
		}
	});

	/**
	 * Flag that indicates whether the current candidate is
	 * the leader.
	 */
	private volatile AtomicBoolean isLeader = new AtomicBoolean(false);
	
	/**
	 * Future returned by submitting an {@link Initiator} to {@link #executorService}.
	 * This is used to cancel leadership.
	 */
	private volatile Future<Void> future;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/** Leader event publisher if set */
	private LeaderEventPublisher leaderEventPublisher;
	
	/**
	 * Construct a {@link LeaderInitiator}.
	 *
	 * @param client     {@link EtcdClient} instance
	 * @param candidate  leadership election candidate
	 * @param namespace	 Etcd namespace
	 */
	public LeaderInitiator(EtcdClient client, Candidate candidate, String namespace) {
		this.client = client;
		this.candidate = candidate;
		this.namespace = namespace;
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (!running) {
			running = true;
			future = executorService.submit(new Initiator());
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public synchronized void stop() {
		if (running) {
			running = false;
			future.cancel(true);
		}
	}

	/**
	 * @return true if leadership election for this {@link #candidate} is running
	 */
	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	@Override
	public void destroy() throws Exception {
		stop();
		executorService.shutdown();
	}
	
	/**
	 * Sets the {@link LeaderEventPublisher}.
	 * 
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}
	
	/**
	 * Callable that manages the etcd heart beats for leadership election.
	 */
	class Initiator implements Callable<Void> {
		
		/**
		 * The base etcd path where candidate id is to be stored.
		 */
		private final String basePath;
		
		/**
		 * The {@link EtcdContext}.
		 */
		private final EtcdContext context;
		
		/**
		 * Executor service for running {@link GrantNotifier} daemon.
		 */
		private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Etcd-Leadership-GrantNotifier");
				thread.setDaemon(true);
				return thread;
			}
		});

		/**
		 * Future returned by submitting an {@link GrantNotifier} to {@link #notificationExecutor}.
		 * This is used to notify leadership revocation to the {@link GrantNotifier}.
		 */
		private volatile Future<Void> notificationFuture;
		
		/**
		 * CountDownLatch initialized on beginning the execution of the {@link GrantNotifier} by
		 * the {@link #notificationExecutor}. This latch is counted down either on finishing the
		 * notification task normally or on cancellation.
		 */
		private volatile CountDownLatch notificationCompletionLatch;
		
		/**
		 * Executor service for running revocation task.
		 */
		private final ExecutorService revocationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "Etcd-Leadership-Revocation");
				thread.setDaemon(true);
				return thread;
			}
		});
		
		/**
		 * Construct a {@link Initiator}.
		 */
		public Initiator() {
			basePath = (namespace == null ? DEFAULT_NAMESPACE : namespace) + "/" + candidate.getRole();
			context = new EtcdContext();
		}
		
		@Override
		public Void call() {
			while (running) {
				try {
					if (isLeader.get()) {
						sendHeartBeat();
					}
					else {
						tryAcquire();
					}
					Thread.sleep(HEART_BEAT_SLEEP);
				}
				catch (InterruptedException e) {
					if (isLeader.get()) {
						initiateRevocation(true);
					}
				}
				catch (IOException | TimeoutException e) {
					LoggerFactory.getLogger(getClass()).warn("Exception occurred while trying to access etcd", e);
					// Continue
				}
			}
			cleanShutdown();
			return null;
		}

		/**
		 * Sends a heart beat to maintain leadership by refreshing the ttl of the etcd key.
		 * If the key has a different value during the call, it is assumed that the current
		 * candidate's leadership is revoked.
		 * 
		 * @throws IOException	if the call to {@link EtcdClient} throws a {@link IOException}.
		 * @throws TimeoutException	if the call to {@link EtcdClient} throws a {@link TimeoutException}.
		 */
		private void sendHeartBeat() throws IOException, TimeoutException {
			try {
				client.put(basePath, candidate.getId()).ttl(TTL).prevValue(candidate.getId()).send().get();
			}
			catch (EtcdException e) {
				initiateRevocation(false);
			}
		}
		
		/**
		 * Tries to acquire leadership by posting the candidate's id to etcd. If the etcd call
		 * is successful, it is assumed that the current candidate is now leader.
		 * 
		 * @throws IOException	if the call to {@link EtcdClient} throws a {@link IOException}.
		 * @throws TimeoutException	if the call to {@link EtcdClient} throws a {@link TimeoutException}.
		 */
		private void tryAcquire() throws IOException, TimeoutException {
			try {
				client.put(basePath, candidate.getId()).ttl(TTL).prevExist(false).send().get();
				notifyGranted();
			}
			catch (EtcdException e) {
				// Keep trying
			}
		}

		/**
		 * Notifies that the candidate has acquired leadership.
		 */
		private void notifyGranted() {
			isLeader.set(true);
			if (leaderEventPublisher != null) {
				leaderEventPublisher.publishOnGranted(LeaderInitiator.this, context);
			}
			notificationFuture = notificationExecutor.submit(new GrantNotifier());
		}

		/**
		 * Cancels the current {@link GrantNotifier} execution if running and
		 * initiates a {@link RevocationTask}.
		 * 
		 * @param deleteEtcdKey	whether the current candidate's key on etcd
		 * should be deleted after {@link RevocationTask} has performed all
		 * notifications.
		 */
		private void initiateRevocation(boolean deleteEtcdKey) {
			notificationFuture.cancel(true);
			revocationExecutor.submit(new RevocationTask(deleteEtcdKey));
		}

		/**
		 * Tries to delete the candidate's entry from etcd.
		 */
		private void tryDeleteCandidateEntry() {
			try {
				client.delete(basePath).prevValue(candidate.getId()).send().get();
			}
			catch (EtcdException e) {
				LoggerFactory.getLogger(getClass()).warn("Exception occurred while trying to delete candidate key", e); 
			}
			catch (IOException | TimeoutException e) {
				LoggerFactory.getLogger(getClass()).warn("Exception occurred while trying to access etcd", e);
			}
		}

		/**
		 * Shuts down {@link #notificationExecutor} and {@link #revocationExecutor}
		 * and waits for {@link #revocationExecutor} to finish running all tasks
		 * before shutting down the {@link EtcdClient}.
		 */
		private void cleanShutdown() {
			notificationExecutor.shutdown();
			revocationExecutor.shutdown();
			try {
				// No need to wait for notificationExecutor to finish all its tasks as
				// the RevocationTask is already doing that. Termination of the
				// revocationExecutor shouldn't ideally take more than a few seconds.
				revocationExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			closeClient();
		}

		/**
		 * Closes the {@link EtcdClient}.
		 */
		private void closeClient() {
			if (client != null) {
				try {
					client.close();
				}
				catch (IOException e) {
					LoggerFactory.getLogger(getClass()).warn("Exception occurred while closing etcd client", e);
				}
			}
		}
		
		/**
		 * Grant event notification callable.
		 */
		class GrantNotifier implements Callable<Void> {

			@Override
			public Void call() {
				notificationCompletionLatch = new CountDownLatch(1);
				try {
					candidate.onGranted(context);
				}
				catch (InterruptedException e) {
					// On cancellation of the GrantNotifier execution.
				}
				finally {
					notificationCompletionLatch.countDown();
				}
				return null;
			}
			
		}
		
		/**
		 * Task that waits for {@link GrantNotifier} to complete before publishing
		 * revocation events and resetting key on Etcd.
		 */
		class RevocationTask implements Callable<Void> {

			/**
			 * Flag that indicates whether the current candidate's key on
			 * etcd should be deleted after performing all notifications.
			 */
			private final boolean deleteEtcdKey;

			/**
			 * Construct a {@link RevocationTask}.
			 * 
			 * @param deleteEtcdKey	whether the current candidate's key on
			 * etcd should be deleted after performing all notifications.
			 */
			public RevocationTask(boolean deleteEtcdKey) {
				this.deleteEtcdKey = deleteEtcdKey;
			}

			@Override
			public Void call() {
				try {
					notificationCompletionLatch.await();
				}
				catch (InterruptedException e) {
					// On shutdown of the executor running this RevocationTask.
				}
				// At any given point in time, multiple RevocationTaskS may be
				// lined up at the revocationExecutor. Therefore, we need to
				// test the state of leadership before actually publishing to
				// prevent generation of multiple revocation notifications.
				if (isLeader.compareAndSet(true, false)) {
					if (leaderEventPublisher != null) {
						leaderEventPublisher.publishOnRevoked(LeaderInitiator.this, context);
					}
					candidate.onRevoked(context);
					if (deleteEtcdKey) {
						tryDeleteCandidateEntry();
					}
				}
				return null;
			}
			
		}

		/**
		 * Implementation of leadership context backed by Etcd.
		 */
		class EtcdContext implements Context {

			@Override
			public boolean isLeader() {
				return isLeader.get();
			}

			@Override
			public void yield() {
				if (isLeader.get()) {
					initiateRevocation(true);
				}
			}

			@Override
			public String toString() {
				return String.format("EtcdContext{role=%s, id=%s, isLeader=%s}",
						candidate.getRole(), candidate.getId(), isLeader());
			}
			
		}

	}

}
