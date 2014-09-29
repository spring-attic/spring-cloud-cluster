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
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.test.SimpleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Sample Spring Boot application that demonstrates leader election
 * with the Spring Cloud leadership API using Curator/Zookeeper
 * as the underlying implementation.
 *
 * @author Patrick Peralta
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class ZKTestApplication {
	private static final Logger logger = LoggerFactory.getLogger(ZKTestApplication.class);

	/**
	 * Main bootstrap method.
	 */
	public static void main(String[] args) {
		SpringApplication.run(ZKTestApplication.class, args);
	}

	/**
	 * @return candidate for leader election
	 */
	@Bean
	public Candidate candidate() {
		return new SimpleCandidate();
	}

	/**
	 * @return Curator/ZooKeeper connection; currently hardcoded to {@code localhost:2818}
	 */
	@Bean(initMethod = "start", destroyMethod = "close")
	public CuratorFramework curatorClient() {
		return CuratorFrameworkFactory.builder()
				.defaultData(new byte[0])
				.retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.connectString("localhost:2181")
				.build();
	}

	/**
	 * @return Curator/ZooKeeper leader election utility
	 */
	@Bean
	public LeaderInitiator initiator() {
		return new LeaderInitiator(curatorClient(), candidate());
	}

}
