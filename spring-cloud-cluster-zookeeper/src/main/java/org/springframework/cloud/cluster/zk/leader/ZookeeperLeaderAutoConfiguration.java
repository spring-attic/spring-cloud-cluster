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
package org.springframework.cloud.cluster.zk.leader;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.LeaderElectionProperties;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for zookeeper leader election.
 * 
 * @author Janne Valkealahti
 *
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.cluster.leader.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(name = "zookeeperLeaderInitiator")
public class ZookeeperLeaderAutoConfiguration {

	@Configuration
	@EnableConfigurationProperties({ LeaderElectionProperties.class, ZookeeperProperties.class })
	@ConditionalOnProperty(value = "spring.cloud.cluster.zookeeper.enabled", matchIfMissing = true)
	@Import(LeaderEventPublisherConfiguration.class)
	protected static class RuntimeConfig {

		@Autowired
		private LeaderElectionProperties lep;
		
		@Autowired
		private ZookeeperProperties zkp;
		
		@Autowired
		private LeaderEventPublisher publisher;
		
		@Bean
		public Candidate zookeeperLeaderCandidate() {
			return new DefaultCandidate(lep.getId(), lep.getRole());
		}

		@Bean
		public CuratorFramework curatorClient() throws Exception {
			CuratorFramework client = CuratorFrameworkFactory.builder().defaultData(new byte[0])
					.retryPolicy(new ExponentialBackoffRetry(1000, 3))
					.connectString(zkp.getConnect()).build();
			return client;
		}

		@Bean
		public LeaderInitiator zookeeperLeaderInitiator() throws Exception {
			LeaderInitiator initiator = new LeaderInitiator(curatorClient(),
					zookeeperLeaderCandidate(), zkp.getNamespace());
			initiator.setLeaderEventPublisher(publisher);
			return initiator;
		}	
		
	}

}
