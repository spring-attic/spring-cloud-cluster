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
 * Tests for {@link EtcdLeaderAutoConfiguration}.
 * 
 * @author Venil Noronha
 */
public class EtcdLeaderAutoConfigurationTests extends AbstractLeaderAutoConfigurationTests {

	@Test
	public void testDefaults() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, EtcdLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("etcdInstance"), is(true));
		assertThat(context.containsBean("etcdLeaderInitiator"), is(true));
		assertThat(context.containsBean("etcdLeaderCandidate"), is(true));
	}

	@Test
	public void testDisabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.cloud.cluster.etcd.leader.enabled:false"
		);
		context.register(LeaderAutoConfiguration.class, EtcdLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("etcdInstance"), is(false));
		assertThat(context.containsBean("etcdLeaderInitiator"), is(false));
		assertThat(context.containsBean("etcdLeaderCandidate"), is(false));
	}
	
	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.cloud.cluster.leader.enabled:false",
			"spring.cloud.cluster.etcd.leader.enabled:true"
		);
		context.register(LeaderAutoConfiguration.class, EtcdLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("etcdInstance"), is(false));
		assertThat(context.containsBean("etcdLeaderInitiator"), is(false));
		assertThat(context.containsBean("etcdLeaderCandidate"), is(false));
	}
	
	@Test
	public void testEnabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
			"spring.cloud.cluster.etcd.leader.enabled:true"
		);
		context.register(LeaderAutoConfiguration.class, EtcdLeaderAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("etcdInstance"), is(true));
		assertThat(context.containsBean("etcdLeaderInitiator"), is(true));
		assertThat(context.containsBean("etcdLeaderCandidate"), is(true));
	}

}
