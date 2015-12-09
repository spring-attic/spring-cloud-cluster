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

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.cluster.etcd.EtcdClusterProperties;
import org.springframework.cloud.cluster.etcd.leader.LeaderInitiator;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.LeaderElectionProperties;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import mousio.etcd4j.EtcdClient;

/**
 * Auto-configuration for etcd leader election.
 * 
 * @author Venil Noronha
 */
@Configuration
@ConditionalOnClass(LeaderInitiator.class)
@ConditionalOnProperty(value = { "spring.cloud.cluster.leader.enabled",
		"spring.cloud.cluster.etcd.leader.enabled" }, matchIfMissing = true)
@ConditionalOnMissingBean(name = "etcdLeaderInitiator")
@EnableConfigurationProperties({ LeaderElectionProperties.class,
		EtcdClusterProperties.class })
@AutoConfigureAfter(LeaderAutoConfiguration.class)
public class EtcdLeaderAutoConfiguration {

	@Autowired
	private LeaderElectionProperties lep;

	@Autowired
	private EtcdClusterProperties ecp;

	@Autowired
	private LeaderEventPublisher publisher;

	@Bean
	public Candidate etcdLeaderCandidate() {
		return new DefaultCandidate(lep.getId(), lep.getRole());
	}

	@Bean
	public EtcdClient etcdInstance() {
		String[] uriList = StringUtils.commaDelimitedListToStringArray(ecp.getConnect());
		URI[] uris = new URI[uriList.length];
		for (int i = 0; i < uriList.length; i ++) {
			uris[i] = URI.create(uriList[i]);
		}
		return new EtcdClient(uris);
	}

	@Bean
	public LeaderInitiator etcdLeaderInitiator() {
		LeaderInitiator initiator = new LeaderInitiator(etcdInstance(), etcdLeaderCandidate(), ecp.getNamespace());
		initiator.setLeaderEventPublisher(publisher);
		return initiator;
	}

}
