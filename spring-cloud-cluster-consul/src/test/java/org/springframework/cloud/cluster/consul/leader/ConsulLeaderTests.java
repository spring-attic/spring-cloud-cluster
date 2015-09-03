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
package org.springframework.cloud.cluster.consul.leader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.cloud.cluster.consul.ConsulClusterProperties;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.event.AbstractLeaderEvent;
import org.springframework.cloud.cluster.leader.event.DefaultLeaderEventPublisher;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecwid.consul.v1.ConsulClient;

/**
 * Tests for Consul leader election.
 *
 * @author Janne Valkealahti
 * @author Patrick Peralta
 * @author Spencer Gibb
 *
 */
public class ConsulLeaderTests {

	@Test
	public void testSimpleLeader() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = null;
		try {
			ctx = new AnnotationConfigApplicationContext(ConsulTestConfig.class);
			TestCandidate candidate = ctx.getBean(TestCandidate.class);
			TestEventListener listener = ctx.getBean(TestEventListener.class);
			assertThat("candidate not granted leadership", candidate.onGrantedLatch.await(5, TimeUnit.SECONDS), is(true));
			assertThat("leader event not received", listener.onEventLatch.await(5, TimeUnit.SECONDS), is(true));
			assertThat("wrong number of leader events", listener.events.size(), is(1));
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}
	}

	@Configuration
	static class ConsulTestConfig {
		@Bean
		public TestCandidate candidate() {
			return new TestCandidate();
		}

		@Bean
		public ConsulClient consulClient() throws Exception {
			return new ConsulClient("localhost", 8500);
		}

		@Bean
		public ConsulLeaderInitiator consulLeaderInitiator() throws Exception {
			ConsulClusterProperties properties = new ConsulClusterProperties();
			properties.getLeader().getSession().setTtl("10s");
			ConsulLeaderInitiator initiator = new ConsulLeaderInitiator(consulClient(), candidate(), properties);
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

	static class TestCandidate extends DefaultCandidate {

		CountDownLatch onGrantedLatch = new CountDownLatch(1);
		String sessionId;

		@Override
		public void onGranted(Context context) {
			onGrantedLatch.countDown();
			super.onGranted(context);

			ConsulLeaderInitiator.ConsulContext ctx = (ConsulLeaderInitiator.ConsulContext) context;
			sessionId = ctx.getSessionId();
		}

	}
	
	static class TestEventListener implements ApplicationListener<AbstractLeaderEvent> {

		CountDownLatch onEventLatch = new CountDownLatch(1);
		
		ArrayList<AbstractLeaderEvent> events = new ArrayList<>();
		
		@Override
		public void onApplicationEvent(AbstractLeaderEvent event) {
			events.add(event);
			onEventLatch.countDown();
		}
		
	}

}
