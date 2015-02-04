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
package org.springframework.cloud.cluster.hazelcast;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for hazelcast leader election.
 * 
 * @author Janne Valkealahti
 *
 */
@ConfigurationProperties(value = "spring.cloud.cluster.hazelcast")
public class HazelcastClusterProperties {
	
	/** xml config location for hazelcast configuration. */
	private Resource configLocation;

	/** hazelcast leader properties. */
	private HazelcastLeaderProperties leader = new HazelcastLeaderProperties();
	
	public Resource getConfigLocation() {
		return configLocation;
	}
	
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}
	
	public HazelcastLeaderProperties getLeader() {
		return leader;
	}
	
	public void setLeader(HazelcastLeaderProperties leader) {
		this.leader = leader;
	}
	
	public static class HazelcastLeaderProperties {

		/** if hazelcast leader election is enabled. */
		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}
		
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}
	
}
