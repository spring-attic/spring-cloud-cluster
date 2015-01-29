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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;

/**
 * Tests for {@link HazelcastLeaderAutoConfiguration}.
 * 
 * @author Janne Valkealahti
 *
 */
public class HazelcastLeaderAutoConfigurationTests extends AbstractLeaderAutoConfigurationTests {

	@Test
	public void testDefaults() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(true));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(true));
	}

	@Test
	public void testDisabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.cloud.cluster.hazelcast.leader.enabled:false");
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(false));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(false));
	}
	
	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.leader.enabled:false",
						"spring.cloud.cluster.hazelcast.leader.enabled:true");
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(false));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(false));
	}
	
	@Test
	public void testEnabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.hazelcast.leader.enabled:true");
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(true));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(true));
	}
	
	@Test
	public void testOverrideConfig() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class, OverrideConfig.class);
		context.refresh();
		
		Config config = context.getBean("hazelcastConfig", Config.class);
		assertThat(config, notNullValue());
		assertThat(config.getProperty("foo"), is("bar"));
		assertThat(config.getProperty("bar"), nullValue());
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(true));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(true));
	}

	@Test
	public void testXmlConfig() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.cloud.cluster.hazelcast.config-location:classpath:/foobar.xml");
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class);
		context.refresh();
		
		Config config = context.getBean("hazelcastConfig", Config.class);
		assertThat(config, notNullValue());
		assertThat(config.getProperty("foo"), is("bar"));
		assertThat(config.getProperty("bar"), is("foo"));
		
		assertThat(context.containsBean("hazelcastLeaderInitiator"), is(true));
		assertThat(context.containsBean("hazelcastLeaderCandidate"), is(true));
	}
	
	@Configuration
	protected static class OverrideConfig {
		
		@Bean
		public Config hazelcastConfig() {
			Config config = new Config();
			config.setProperty("foo", "bar");
			return config;
		}
		
	}
	
}
