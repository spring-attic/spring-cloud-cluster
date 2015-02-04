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
package org.springframework.cloud.cluster.zk;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for zookeeper leader election.
 * 
 * @author Janne Valkealahti
 *
 */
@ConfigurationProperties(value = "spring.cloud.cluster.zookeeper")
public class ZookeeperClusterProperties {

	/** base zookeeper namespace path. */
	private String namespace;
	
	/** connect string for zookeeper. */
	private String connect = "localhost:2181";

	/** zookeeper leader properties. */
	private ZookeeperLeaderProperties leader = new ZookeeperLeaderProperties();
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getConnect() {
		return connect;
	}

	public void setConnect(String connect) {
		this.connect = connect;
	}
	
	public ZookeeperLeaderProperties getLeader() {
		return leader;
	}
	
	public void setLeader(ZookeeperLeaderProperties leader) {
		this.leader = leader;
	}
	
	public static class ZookeeperLeaderProperties {

		/** if zookeeper leader election is enabled. */
		private boolean enabled = true;
	
		public boolean isEnabled() {
			return enabled;
		}
		
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		
	}

}
