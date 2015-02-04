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
package org.springframework.cloud.cluster.autoconfigure.leader;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.cluster.hazelcast.HazelcastClusterProperties;
import org.springframework.cloud.cluster.hazelcast.leader.LeaderInitiator;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.LeaderElectionProperties;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Auto-configuration for hazelcast leader election.
 * 
 * @author Janne Valkealahti
 *
 */
@Configuration
@ConditionalOnClass(LeaderInitiator.class)
@ConditionalOnProperty(value = { "spring.cloud.cluster.leader.enabled",
		"spring.cloud.cluster.hazelcast.leader.enabled" }, matchIfMissing = true)
@ConditionalOnMissingBean(name = "hazelcastLeaderInitiator")
@EnableConfigurationProperties({ LeaderElectionProperties.class,
		HazelcastClusterProperties.class })
@AutoConfigureAfter(LeaderAutoConfiguration.class)
public class HazelcastLeaderAutoConfiguration {

	@Autowired
	private LeaderElectionProperties lep;

	@Autowired
	private HazelcastClusterProperties hp;

	@Autowired
	private LeaderEventPublisher publisher;

	@Bean
	public Candidate hazelcastLeaderCandidate() {
		return new DefaultCandidate(lep.getId(), lep.getRole());
	}

	@Bean
	public HazelcastInstance hazelcastInstance() {
		return Hazelcast.newHazelcastInstance(hazelcastConfig());
	}

	@Bean
	public LeaderInitiator hazelcastLeaderInitiator() {
		LeaderInitiator initiator = new LeaderInitiator(hazelcastInstance(),
				hazelcastLeaderCandidate());
		initiator.setLeaderEventPublisher(publisher);
		return initiator;
	}

	@Bean
	public Config hazelcastConfig() {
		Resource location = hp.getConfigLocation();
		if (location != null && location.exists()) {
			try {
				return new XmlConfigBuilder(hp.getConfigLocation()
						.getInputStream()).build();
			} catch (IOException e) {
				throw new IllegalArgumentException(
						"Unable to use config location " + location, e);
			}
		} else {
			return new Config();
		}
	}

}
