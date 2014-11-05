/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.cloud.cluster.zk.leader;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.context.Lifecycle;

/**
 * Bootstrap leadership {@link org.springframework.cloud.cluster.leader.Candidate candidates}
 * with ZooKeeper/Curator. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * @author Patrick Peralta
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	/**
	 * Curator client.
	 */
	private final CuratorFramework client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Curator utility for selecting leaders.
	 */
	private volatile LeaderSelector leaderSelector;

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile boolean running;

	/**
	 * Construct a {@link LeaderInitiator}.
	 *
	 * @param client     Curator client
	 * @param candidate  leadership election candidate
	 */
	public LeaderInitiator(CuratorFramework client, Candidate candidate) {
		this.client = client;
		this.candidate = candidate;
	}

	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (!running) {
			leaderSelector = new LeaderSelector(client, buildLeaderPath(), new LeaderListener());
			leaderSelector.setId(candidate.getId());
			leaderSelector.autoRequeue();
			leaderSelector.start();

			running = true;
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public synchronized void stop() {
		if (running) {
			leaderSelector.close();
			running = false;
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
	}

	/**
	 * @return the ZooKeeper path used for leadership election by Curator
	 */
	private String buildLeaderPath() {
		return String.format("/spring-cloud/leader/%s", candidate.getRole());
	}

	/**
	 * Implementation of Curator leadership election listener.
	 */
	class LeaderListener extends LeaderSelectorListenerAdapter {

		@Override
		public void takeLeadership(CuratorFramework framework) throws Exception {
			CuratorContext context = new CuratorContext();

			try {
				candidate.onGranted(context);

				// when this method exits, the leadership will be revoked;
				// therefore this thread needs to be held up until the
				// candidate is no longer leader
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				candidate.onRevoked(context);
			}
		}
	}

	/**
	 * Implementation of leadership context backed by Curator.
	 */
	class CuratorContext implements Context {

		@Override
		public String getRole() {
			return candidate.getRole();
		}

		@Override
		public String getId() {
			return candidate.getId();
		}

		@Override
		public boolean isLeader() {
			return leaderSelector.hasLeadership();
		}

		@Override
		public void yield() {
			leaderSelector.interruptLeadership();
		}

		@Override
		public String toString() {
			return String.format("CuratorContext{role=%s, id=%s, isLeader=%s}",
					getRole(), getId(), isLeader());
		}

	}
}
