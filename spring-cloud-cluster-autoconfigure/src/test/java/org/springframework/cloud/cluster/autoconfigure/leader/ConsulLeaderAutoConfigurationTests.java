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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecwid.consul.v1.ConsulClient;

/**
 * Tests for {@link ConsulLeaderAutoConfiguration}.
 * 
 * @author Janne Valkealahti
 * @author Spencer Gibb
 *
 */
public class ConsulLeaderAutoConfigurationTests extends AbstractLeaderAutoConfigurationTests {

	@Test
	public void testDefaults() throws Exception {
		setupEnvironment();

		assertThat(context.containsBean("consulLeaderInitiator"), is(true));
		assertThat(context.containsBean("consulLeaderCandidate"), is(true));
	}

	@Test
	public void testEnabled() throws Exception {
		setupEnvironment("spring.cloud.cluster.consul.leader.enabled:true");

		assertThat(context.containsBean("consulLeaderInitiator"), is(true));
		assertThat(context.containsBean("consulLeaderCandidate"), is(true));
	}

	@Test
	public void testDisabled() throws Exception {
		setupEnvironment("spring.cloud.cluster.consul.leader.enabled:false");

		assertThat(context.containsBean("consulLeaderInitiator"), is(false));
		assertThat(context.containsBean("consulLeaderCandidate"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		setupEnvironment("spring.cloud.cluster.leader.enabled:false",
				"spring.cloud.cluster.consul.leader.enabled:true");

		assertThat(context.containsBean("consulLeaderInitiator"), is(false));
		assertThat(context.containsBean("consulLeaderCandidate"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabledConsulEnabled() throws Exception {
		setupEnvironment("spring.cloud.cluster.leader.enabled:false");

		assertThat(context.containsBean("consulLeaderInitiator"), is(false));
		assertThat(context.containsBean("consulLeaderCandidate"), is(false));
	}

	private void setupEnvironment(String... pairs) {
		EnvironmentTestUtils.addEnvironment(this.context, pairs);
		context.register(LeaderAutoConfiguration.class, ConsulClientConfig.class, ConsulLeaderAutoConfiguration.class);
		context.refresh();
	}

	@Configuration
	static class ConsulClientConfig {
		@Bean
		public ConsulClient consulClient() {
			return new ConsulClient("localhost", 8500);
		}
	}

}
