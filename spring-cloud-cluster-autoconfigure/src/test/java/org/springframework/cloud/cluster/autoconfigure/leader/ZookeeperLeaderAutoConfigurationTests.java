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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;

/**
 * Tests for {@link ZookeeperLeaderAutoConfiguration}.
 * 
 * @author Janne Valkealahti
 *
 */
public class ZookeeperLeaderAutoConfigurationTests extends AbstractLeaderAutoConfigurationTests {

	@Override
	protected ZookeeperTestingServerWrapper setupZookeeperTestingServer() throws Exception {
		return new ZookeeperTestingServerWrapper();
	}
	
	@Test
	public void testDefaults() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(true));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(true));
	}

	@Test
	public void testEnabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.zookeeper.leader.enabled:true",
						"spring.cloud.cluster.zookeeper.connect:localhost:" + zookeeper.getPort());
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(true));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(true));
	}

	@Test
	public void testDisabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.zookeeper.leader.enabled:false");
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(false));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.leader.enabled:false",
						"spring.cloud.cluster.zookeeper.leader.enabled:true");
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(false));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabledZkEnabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.leader.enabled:false");
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("zookeeperLeaderInitiator"), is(false));
		assertThat(context.containsBean("zookeeperLeaderCandidate"), is(false));
	}
	
}
