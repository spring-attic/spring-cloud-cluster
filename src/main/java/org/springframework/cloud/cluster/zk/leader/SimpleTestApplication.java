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

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Patrick Peralta
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SimpleTestApplication {
	private static final Logger logger = LoggerFactory.getLogger(SimpleTestApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SimpleTestApplication.class, args);
	}

	@Bean
	public Candidate candidate() {
		return new SimpleCandidate();
	}

	@Bean
	public CuratorFramework curatorClient() {
		CuratorFramework client = CuratorFrameworkFactory.builder()
				.defaultData(new byte[0])
				.retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.connectString("localhost:2181")
				.build();

		client.start(); // todo ???

		return client;
	}

	@Bean
	public LeaderInitiator initiator() {
		return new LeaderInitiator(curatorClient(), candidate());
	}

	public static class SimpleCandidate implements Candidate, ApplicationContextAware {

		private final String role = "leader";

		private volatile ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public String getRole() {
			return role;
		}

		@Override
		public String getId() {
			return applicationContext.getId();
		}

		@Override
		public void onGranted(Context ctx) throws InterruptedException {
			logger.info("{} has been granted leadership; context: {}", this, ctx);
		}

		@Override
		public void onRevoked(Context ctx) {
			logger.info("{} leadership has been revoked", this, ctx);
		}

		@Override
		public String toString() {
			return String.format("SimpleCandidate{role=%s, id%s=}", getRole(), getId());
		}

	}
}
