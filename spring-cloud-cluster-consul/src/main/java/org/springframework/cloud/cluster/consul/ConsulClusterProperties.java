/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.cluster.consul;

import com.ecwid.consul.v1.session.model.Session.Behavior;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for zookeeper leader election.
 * 
 * @author Janne Valkealahti
 *
 */
@ConfigurationProperties(value = "spring.cloud.cluster.consul")
public class ConsulClusterProperties {

	/** consul leader properties. */
	private LeaderProperties leader = new LeaderProperties();
	
	public LeaderProperties getLeader() {
		return leader;
	}
	
	public void setLeader(LeaderProperties leader) {
		this.leader = leader;
	}
	
	public static class LeaderProperties {

		/** if consul leader election is enabled. */
		private boolean enabled = true;

		private String namespace = "spring-cloud/leader/";

		private SessionProperties session = new SessionProperties();

		public boolean isEnabled() {
			return enabled;
		}
		
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public SessionProperties getSession() {
			return session;
		}

		public void setSession(SessionProperties session) {
			this.session = session;
		}
	}

	public static class SessionProperties {

		private String ttl;

		private long lockDelay;

		private Behavior behavior;

		private Long renewalDelay;

		public String getTtl() {
			return ttl;
		}

		public void setTtl(String ttl) {
			this.ttl = ttl;
		}

		public Behavior getBehavior() {
			return behavior;
		}

		public void setBehavior(Behavior behavior) {
			this.behavior = behavior;
		}

		public long getLockDelay() {
			return lockDelay;
		}

		public void setLockDelay(long lockDelay) {
			this.lockDelay = lockDelay;
		}

		public Long getRenewalDelay() {
			return renewalDelay;
		}

		public void setRenewalDelay(Long renewalDelay) {
			this.renewalDelay = renewalDelay;
		}
	}

}
