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

package org.springframework.cloud.cluster.hazelcast.leader;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.event.DefaultLeaderEventPublisher;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap leadership {@link org.springframework.cloud.cluster.leader.Candidate candidates}
 * with Hazelcast. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Patrick Peralta
 * @author Gary Russell
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(LeaderInitiator.class);

	/**
	 * Hazelcast client.
	 */
	private final HazelcastInstance client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Executor service for running leadership daemon.
	 */
	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Hazelcast leadership");
			thread.setDaemon(true);
			return thread;
		}
	});

	/**
	 * Future returned by submitting an {@link Initiator} to {@link #executorService}.
	 * This is used to cancel leadership.
	 */
	private volatile Future<Void> future;

	/**
	 * Hazelcast distributed map used for locks.
	 */
	private volatile IMap<String, String> mapLocks;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/**
	 * Leader event publisher.
	 */
	private volatile LeaderEventPublisher leaderEventPublisher = new DefaultLeaderEventPublisher();

	/**
	 * Construct a {@link LeaderInitiator}.
	 *
	 * @param client     Hazelcast client
	 * @param candidate  leadership election candidate
	 */
	public LeaderInitiator(HazelcastInstance client, Candidate candidate) {
		this.client = client;
		this.candidate = candidate;
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (!running) {
			mapLocks = client.getMap("spring-cloud-leader");
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
		Assert.notNull(leaderEventPublisher);
		this.leaderEventPublisher = leaderEventPublisher;
	}

	/**
	 * Callable that manages the acquisition of Hazelcast locks
	 * for leadership election.
	 */
	class Initiator implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			Assert.state(mapLocks != null);
			HazelcastContext context = new HazelcastContext();
			String role = candidate.getRole();
			boolean locked = false;

			while (running) {
				try {
					locked = mapLocks.tryLock(role, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
					if (locked) {
						mapLocks.put(role, candidate.getId());
						leaderEventPublisher.publishOnGranted(LeaderInitiator.this, context, candidate.getRole());
						candidate.onGranted(context);
						Thread.sleep(Long.MAX_VALUE);
					}
				}
				catch (Exception e) {
					// Catch and log any exceptions. This prevents the
					// call method from exiting so that another attempt
					// at acquiring leadership may be made.
					// There is no need to reset the interrupt flag
					// on InterruptedException because the interrupt
					// is handled in the finally block below (just like
					// any other exception)
					logger.warn("Exception caught", e);
				}
				finally {
					if (locked) {
						mapLocks.remove(role);
						mapLocks.unlock(role);
						candidate.onRevoked(context);
						leaderEventPublisher.publishOnRevoked(LeaderInitiator.this, context, candidate.getRole());
						locked = false;
					}
				}
			}
			return null;
		}

	}

	/**
	 * Implementation of leadership context backed by Hazelcast.
	 */
	class HazelcastContext implements Context {

		@Override
		public boolean isLeader() {
			return mapLocks != null && mapLocks.isLocked(candidate.getRole());
		}

		@Override
		public void yield() {
			if (future != null) {
				future.cancel(true);
			}
		}

		@Override
		public String toString() {
			return String.format("HazelcastContext{role=%s, id=%s, isLeader=%s}",
					candidate.getRole(), candidate.getId(), isLeader());
		}
	}
}
