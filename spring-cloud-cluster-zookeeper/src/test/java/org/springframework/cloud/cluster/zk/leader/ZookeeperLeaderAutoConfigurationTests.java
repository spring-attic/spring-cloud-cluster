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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests for {@link ZookeeperLeaderAutoConfiguration}.
 * 
 * @author Janne Valkealahti
 *
 */
public class ZookeeperLeaderAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;
	
	private TestingServerWrapper server;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
		if (server != null) {
			try {
				server.destroy();
			} catch (Exception e) {
			}
			server = null;
		}
	}
	
	@Test
	public void testDefaults() throws Exception {
		server = new TestingServerWrapper();
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(true));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(true));
	}

	@Test
	public void testEnabled() throws Exception {
		server = new TestingServerWrapper();
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.zookeeper.enabled:true",
						"spring.cloud.cluster.zookeeper.connect:localhost:" + server.getPort());
		context.register(ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(true));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(true));
	}

	@Test
	public void testDisabled() throws Exception {
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.zookeeper.enabled:false");
		context.register(ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(false));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.leader.enabled:false",
						"spring.cloud.cluster.zookeeper.enabled:true");
		context.register(ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(false));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(false));
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
	
}
