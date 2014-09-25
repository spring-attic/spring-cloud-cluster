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

import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
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
public class SimpleTestApplication {
	private static final Logger logger = LoggerFactory.getLogger(SimpleTestApplication.class);

	/**
	 * Main bootstrap method.
	 */
	public static void main(String[] args) {
		SpringApplication.run(SimpleTestApplication.class, args);
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

	/**
	 * Simple {@link Candidate} for leadership. This implementation simply
	 * logs when it is elected and when its leadership is revoked.
	 */
	public static class SimpleCandidate implements Candidate {

		private final String role = "leader";

		private final String id = UUID.randomUUID().toString();

		private volatile Context leaderContext;

		@Override
		public String getRole() {
			return role;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void onGranted(Context ctx) throws InterruptedException {
			logger.info("{} has been granted leadership; context: {}", this, ctx);
			leaderContext = ctx;
		}

		@Override
		public void onRevoked(Context ctx) {
			logger.info("{} leadership has been revoked", this, ctx);
		}

		public Context getLeaderContext() {
			// TODO: this is being exposed so that the CRaSSH command
			// can access the context to cancel leadership; should this
			// be exposed to the Candidate interface? Or should the interface
			// itself include a "renounce" method?
			return leaderContext;
		}

		@Override
		public String toString() {
			return String.format("SimpleCandidate{role=%s, id=%s}", getRole(), getId());
		}

	}
}
