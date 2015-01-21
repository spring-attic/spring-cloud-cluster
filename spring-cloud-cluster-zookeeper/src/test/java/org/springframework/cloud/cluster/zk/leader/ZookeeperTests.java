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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.event.AbstractLeaderEvent;
import org.springframework.cloud.cluster.leader.event.DefaultLeaderEventPublisher;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for zookeeper leader election.
 *
 * @author Janne Valkealahti
 * @author Patrick Peralta
 *
 */
public class ZookeeperTests {

	@Test
	public void testSimpleLeader() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ZkServerConfig.class, Config1.class);
		TestCandidate candidate = ctx.getBean(TestCandidate.class);
		TestEventListener listener = ctx.getBean(TestEventListener.class);
		assertThat(candidate.onGrantedLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(listener.onEventLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(listener.events.size(), is(1));
		ctx.close();
	}

	@Configuration
	static class ZkServerConfig {

		@Bean
		public TestingServerWrapper testingServerWrapper() throws Exception {
			return new TestingServerWrapper();
		}

	}

	@Configuration
	static class Config1 {

		@Autowired
		TestingServerWrapper testingServerWrapper;

		@Bean
		public TestCandidate candidate() {
			return new TestCandidate();
		}

		@Bean(destroyMethod = "close")
		public CuratorFramework curatorClient() throws Exception {
			CuratorFramework client = CuratorFrameworkFactory.builder().defaultData(new byte[0])
					.retryPolicy(new ExponentialBackoffRetry(1000, 3))
					.connectString("localhost:" + testingServerWrapper.getPort()).build();
			// for testing we start it here, thought initiator
			// is trying to start it if not already done
			client.start();
			return client;
		}

		@Bean
		public LeaderInitiator initiator() throws Exception {
			LeaderInitiator initiator = new LeaderInitiator(curatorClient(), candidate());
			initiator.setLeaderEventPublisher(leaderEventPublisher());
			return initiator;
		}
		
		@Bean
		public LeaderEventPublisher leaderEventPublisher() {
			return new DefaultLeaderEventPublisher();
		}		
		
		@Bean
		public TestEventListener testEventListener() {
			return new TestEventListener();
		}

	}

	static class TestingServerWrapper implements DisposableBean {

		TestingServer testingServer;

		public TestingServerWrapper() throws Exception {
			this.testingServer = new TestingServer(true);
		}

		@Override
		public void destroy() throws Exception {
			try {
				testingServer.close();
			}
			catch (IOException e) {
			}
		}

		public int getPort() {
			return testingServer.getPort();
		}

	}

	static class TestCandidate extends DefaultCandidate {

		CountDownLatch onGrantedLatch = new CountDownLatch(1);

		@Override
		public void onGranted(Context ctx) {
			onGrantedLatch.countDown();
			super.onGranted(ctx);
		}

	}
	
	static class TestEventListener implements ApplicationListener<AbstractLeaderEvent> {

		CountDownLatch onEventLatch = new CountDownLatch(1);
		
		ArrayList<AbstractLeaderEvent> events = new ArrayList<AbstractLeaderEvent>();
		
		@Override
		public void onApplicationEvent(AbstractLeaderEvent event) {
			events.add(event);
			onEventLatch.countDown();
		}
		
	}

}
