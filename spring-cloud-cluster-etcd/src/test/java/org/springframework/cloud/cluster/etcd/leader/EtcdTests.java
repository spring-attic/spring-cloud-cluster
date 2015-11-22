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
package org.springframework.cloud.cluster.etcd.leader;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.DefaultCandidate;
import org.springframework.cloud.cluster.leader.event.AbstractLeaderEvent;
import org.springframework.cloud.cluster.leader.event.DefaultLeaderEventPublisher;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mousio.etcd4j.EtcdClient;

/**
 * Tests for etcd leader election.
 *
 * @author Venil Noronha
 */
public class EtcdTests {

	@Test
	public void testSimpleLeader() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config1.class);
		TestCandidate candidate = ctx.getBean(TestCandidate.class);
		TestEventListener listener = ctx.getBean(TestEventListener.class);
		assertThat(candidate.onGrantedLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(listener.onEventLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(listener.events.size(), is(1));
		ctx.close();
	}
	
	@Test
	public void testLeaderYield() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(YieldTestConfig.class);
		YieldTestCandidate candidate = ctx.getBean(YieldTestCandidate.class);
		YieldTestEventListener listener = ctx.getBean(YieldTestEventListener.class);
		assertThat(candidate.onGrantedLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(candidate.onRevokedLatch.await(5, TimeUnit.SECONDS), is(true));
		assertThat(listener.onEventsLatch.await(10, TimeUnit.SECONDS), is(true));
		assertThat(listener.events.size(), is(2));
		ctx.close();
	}

	@Configuration
	static class Config1 {

		@Bean
		public TestCandidate candidate() {
			return new TestCandidate();
		}

		@Bean
		public EtcdClient etcdInstance() {
			return new EtcdClient(URI.create("http://localhost:4001"));
		}

		@Bean
		public LeaderInitiator initiator() {
			LeaderInitiator initiator = new LeaderInitiator(etcdInstance(), candidate(), "etcd-test");
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
	
	@Configuration
	static class YieldTestConfig {

		@Bean
		public YieldTestCandidate candidate() {
			return new YieldTestCandidate();
		}

		@Bean
		public EtcdClient etcdInstance() {
			return new EtcdClient(URI.create("http://localhost:4001"));
		}

		@Bean
		public LeaderInitiator initiator() {
			LeaderInitiator initiator = new LeaderInitiator(etcdInstance(), candidate(), "etcd-yield-test");
			initiator.setLeaderEventPublisher(leaderEventPublisher());
			return initiator;
		}

		@Bean
		public LeaderEventPublisher leaderEventPublisher() {
			return new DefaultLeaderEventPublisher();
		}		
		
		@Bean
		public YieldTestEventListener testEventListener() {
			return new YieldTestEventListener();
		}
		
	}
	
	static class YieldTestCandidate extends DefaultCandidate {

		CountDownLatch onGrantedLatch = new CountDownLatch(1);
		CountDownLatch onRevokedLatch = new CountDownLatch(1);

		@Override
		public void onGranted(Context ctx) {
			onGrantedLatch.countDown();
			ctx.yield();
		}
		
		@Override
		public void onRevoked(Context ctx) {
			onRevokedLatch.countDown();
		}

	}

	static class YieldTestEventListener implements ApplicationListener<AbstractLeaderEvent> {

		CountDownLatch onEventsLatch = new CountDownLatch(2);
		
		ArrayList<AbstractLeaderEvent> events = new ArrayList<AbstractLeaderEvent>();
		
		@Override
		public void onApplicationEvent(AbstractLeaderEvent event) {
			events.add(event);
			onEventsLatch.countDown();
		}
		
	}
	
}
