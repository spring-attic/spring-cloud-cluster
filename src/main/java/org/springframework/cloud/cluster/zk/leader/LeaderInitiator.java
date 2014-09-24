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
 * @author Patrick Peralta
 */
public class LeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {
	private final CuratorFramework client;

	private final Candidate candidate;

	private final LeaderSelector leaderSelector;

	public LeaderInitiator(CuratorFramework client, Candidate candidate) {
		this.client = client;
		this.candidate = candidate;
		this.leaderSelector = new LeaderSelector(client, buildLeaderPath(), new LeaderListener());
		this.leaderSelector.setId(candidate.getId());
	}

	@Override
	public void start() {
		leaderSelector.start();
	}

	@Override
	public void stop() {
		leaderSelector.close();
	}

	@Override
	public boolean isRunning() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	@Override
	public void destroy() throws Exception {
		stop();
	}

	private String buildLeaderPath() {
		return String.format("/spring-cloud/leader/%s", candidate.getRole());
	}

	class LeaderListener extends LeaderSelectorListenerAdapter {

		@Override
		public void takeLeadership(CuratorFramework framework) throws Exception {
			CuratorContext context = new CuratorContext();

			try {
				candidate.onGranted(context);
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				candidate.onRevoked(context);
			}
		}
	}

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
		public void renounce() {
			leaderSelector.interruptLeadership();
		}

		@Override
		public String toString() {
			return String.format("CuratorContext{role=%s, id=%s, isLeader=%s}",
					getRole(), getId(), isLeader());
		}

	}
}
