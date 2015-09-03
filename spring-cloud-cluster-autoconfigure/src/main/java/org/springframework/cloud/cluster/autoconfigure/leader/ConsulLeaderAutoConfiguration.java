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

import com.ecwid.consul.v1.ConsulClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.LeaderElectionProperties;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.cloud.cluster.consul.ConsulClusterProperties;
import org.springframework.cloud.cluster.consul.leader.ConsulLeaderInitiator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for consul leader election.
 * 
 * @author Janne Valkealahti
 * @author Spencer Gibb
 *
 */
@Configuration
@ConditionalOnClass(ConsulLeaderInitiator.class)
@ConditionalOnProperty(value = { "spring.cloud.cluster.consul.leader.enabled",
		"spring.cloud.cluster.leader.enabled" }, matchIfMissing = true)
@ConditionalOnMissingBean(name = "consulLeaderInitiator")
@ConditionalOnBean(ConsulClient.class)
@EnableConfigurationProperties({ LeaderElectionProperties.class,
		ConsulClusterProperties.class })
@AutoConfigureAfter(LeaderAutoConfiguration.class)
public class ConsulLeaderAutoConfiguration {

	@Autowired
	private LeaderElectionProperties lep;

	@Autowired
	private ConsulClusterProperties props;

	@Autowired
	private LeaderEventPublisher publisher;

	@Bean
	public Candidate consulLeaderCandidate() {
		return new DefaultCandidate(lep.getId(), lep.getRole());
	}

	@Bean
	public ConsulLeaderInitiator consulLeaderInitiator(ConsulClient client) throws Exception {
		ConsulLeaderInitiator initiator = new ConsulLeaderInitiator(client,
				consulLeaderCandidate(), props);
		initiator.setLeaderEventPublisher(publisher);
		return initiator;
	}

}
