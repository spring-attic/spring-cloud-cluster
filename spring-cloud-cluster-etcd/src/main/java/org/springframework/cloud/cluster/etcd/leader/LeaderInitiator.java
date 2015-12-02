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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
 * with etcd. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Venil Noronha
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	private final static int TTL = 10;
	private final static int HEART_BEAT_SLEEP = TTL / 2;
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
	 * Executor service for running leadership daemon.
	 */
	private final ExecutorService leaderExecutorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Etcd-Leadership");
			thread.setDaemon(true);
			return thread;
		}
	});
	
	/**
	 * Executor service for running leadership worker daemon.
	 */
	private final ExecutorService workerExecutorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Etcd-Leadership-Worker");
			thread.setDaemon(true);
			return thread;
		}
	});

	/**
	 * Flag that indicates whether the current candidate is
	 * the leader.
	 */
	private volatile boolean isLeader = false;
	
	/**
	 * Flag that indicates whether the current candidate's
	 * leadership should be relinquished.
	 */
	private volatile boolean relinquishLeadership = false;
	
	/**
	 * Future returned by submitting a {@link Initiator} to {@link #leaderExecutorService}.
	 * This is used to cancel leadership.
	 */
	private volatile Future<Void> initiatorFuture;
	
	/**
	 * Future returned by submitting a {@link Worker} to {@link #workerExecutorService}.
	 * This is used to notify leadership revocation.
	 */
	private volatile Future<Void> workerFuture;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/**
	 * Leader event publisher if set.
	 */
	private LeaderEventPublisher leaderEventPublisher;
	
	/**
	 * The {@link EtcdContext} instance.
	 */
	private final EtcdContext context;

	/**
	 * The base etcd path where candidate id is to be stored.
	 */
	private final String baseEtcdPath;
	
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
		this.context = new EtcdContext();
		this.baseEtcdPath = (namespace == null ? DEFAULT_NAMESPACE : namespace) + "/" + candidate.getRole();
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (!running) {
			running = true;
			initiatorFuture = leaderExecutorService.submit(new Initiator());
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
			initiatorFuture.cancel(true);
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
		workerExecutorService.shutdown();
		leaderExecutorService.shutdown();
		workerExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		leaderExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
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
	 * Notifies that the candidate has acquired leadership.
	 */
	private void notifyGranted() {
		isLeader = true;
		if (leaderEventPublisher != null) {
			leaderEventPublisher.publishOnGranted(LeaderInitiator.this, context);
		}
		workerFuture = workerExecutorService.submit(new Worker());
	}
	
	/**
	 * Notifies that the candidate's leadership was revoked.
	 */
	private void notifyRevoked() {
		isLeader = false;
		if (leaderEventPublisher != null) {
			leaderEventPublisher.publishOnRevoked(LeaderInitiator.this, context);
		}
		workerFuture.cancel(true);
	}

	/**
	 * Tries to delete the candidate's entry from etcd.
	 */
	private void tryDeleteCandidateEntry() {
		try {
			client.delete(baseEtcdPath).prevValue(candidate.getId()).send().get();
		}
		catch (EtcdException e) {
			LoggerFactory.getLogger(getClass()).warn("Couldn't delete candidate's entry from etcd", e); 
		}
		catch (IOException | TimeoutException e) {
			LoggerFactory.getLogger(getClass()).warn("Couldn't access etcd", e);
		}
	}

	/**
	 * Callable that invokes {@link Candidate#onGranted(Context)}
	 * when the candidate is granted leadership.
	 */
	class Worker implements Callable<Void> {

		@Override
		public Void call() {
			try {
				candidate.onGranted(context);
			}
			catch (InterruptedException e) {
				// If the candidate's leadership was revoked
				Thread.currentThread().interrupt();
			}
			catch (Throwable t) {
				relinquishLeadership = true;
				LoggerFactory.getLogger(getClass()).error("Some error occurred while "
						+ "executing candidate's grant callback, relinquishing leadership...", t);
			}
			try {
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				// If the candidate's leadership was revoked
			}
			finally {
				candidate.onRevoked(context);
			}
			return null;
		}

	}
	
	/**
	 * Callable that manages the etcd heart beats for leadership election.
	 */
	class Initiator implements Callable<Void> {
		
		@Override
		public Void call() {
			try {
				while (running) {
					if (relinquishLeadership) {
						relinquishLeadership = false;
						relinquishLeadership();
					}
					else if (isLeader) {
						sendHeartBeat();
					}
					else {
						tryAcquire();
					}
					TimeUnit.SECONDS.sleep(HEART_BEAT_SLEEP);
				}
			}
			catch (InterruptedException e) {
				// If the initiator future was cancelled
			}
			finally {
				if (isLeader) {
					relinquishLeadership();
				}
			}
			return null;
		}

		/**
		 * Relinquishes leadership of current candidate by deleting candidate's entry from
		 * etcd and then notifies that the current candidate is no longer leader.
		 */
		private void relinquishLeadership() {
			tryDeleteCandidateEntry();
			notifyRevoked();
		}

		/**
		 * Sends a heart beat to maintain leadership by refreshing the ttl of the etcd key.
		 * If the key has a different value during the call, it is assumed that the current
		 * candidate's leadership is revoked. If access to etcd fails, then the the current
		 * candidate's leadership is relinquished.
		 */
		private void sendHeartBeat() {
			try {
				client.put(baseEtcdPath, candidate.getId()).ttl(TTL).prevValue(candidate.getId()).send().get();
			}
			catch (EtcdException e) {
				notifyRevoked();
			}
			catch (IOException | TimeoutException e) {
				// Couldn't access etcd, therefore, relinquish leadership
				LoggerFactory.getLogger(getClass()).warn("Couldn't access etcd", e);
				notifyRevoked();
			}
		}
		
		/**
		 * Tries to acquire leadership by posting the candidate's id to etcd. If the etcd call
		 * is successful, it is assumed that the current candidate is now leader.
		 */
		private void tryAcquire() {
			try {
				client.put(baseEtcdPath, candidate.getId()).ttl(TTL).prevExist(false).send().get();
				notifyGranted();
			}
			catch (EtcdException e) {
				// Couldn't set the value to current candidate's id, therefore, keep trying.
			}
			catch (IOException | TimeoutException e) {
				// Couldn't access etcd, therefore, keep trying.
				LoggerFactory.getLogger(getClass()).warn("Couldn't access etcd", e);
			}
		}

	}

	/**
	 * Implementation of leadership context backed by Etcd.
	 */
	class EtcdContext implements Context {

		@Override
		public boolean isLeader() {
			return isLeader;
		}

		@Override
		public void yield() {
			if (isLeader) {
				relinquishLeadership = true;
			}
		}

		@Override
		public String toString() {
			return String.format("EtcdContext{role=%s, id=%s, isLeader=%s}",
					candidate.getRole(), candidate.getId(), isLeader());
		}

	}

}
