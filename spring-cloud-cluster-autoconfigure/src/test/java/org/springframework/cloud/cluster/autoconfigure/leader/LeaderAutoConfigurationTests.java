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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.cluster.leader.Candidate;

/**
 * Tests for common leadership concepts.
 * 
 * @author Janne Valkealahti
 * @author Venil Noronha
 */
public class LeaderAutoConfigurationTests extends AbstractLeaderAutoConfigurationTests {

	@Override
	protected ZookeeperTestingServerWrapper setupZookeeperTestingServer() throws Exception {
		return new ZookeeperTestingServerWrapper();
	}

	@Test
	public void testAutowireSingleZookeeperCandidate() {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, ZookeeperLeaderAutoConfiguration.class, Config1.class);
		context.refresh();
		
		Config1 config1 = context.getBean(Config1.class);
		assertThat(config1, notNullValue());
		assertThat(config1.candidate, notNullValue());
	}

	@Test
	public void testAutowireSingleHazelcastCandidate() {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, HazelcastLeaderAutoConfiguration.class, Config1.class);
		context.refresh();
		
		Config1 config1 = context.getBean(Config1.class);
		assertThat(config1, notNullValue());
		assertThat(config1.candidate, notNullValue());		
	}
	
	@Test
	public void testAutowireSingleEtcdCandidate() {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class, EtcdLeaderAutoConfiguration.class, Config1.class);
		context.refresh();
		
		Config1 config1 = context.getBean(Config1.class);
		assertThat(config1, notNullValue());
		assertThat(config1.candidate, notNullValue());		
	}

	@Test
	public void testAutowireMultipleCandidates() {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(LeaderAutoConfiguration.class,
				ZookeeperLeaderAutoConfiguration.class,
				HazelcastLeaderAutoConfiguration.class,
				EtcdLeaderAutoConfiguration.class, Config2.class);
		context.refresh();
		
		Config2 config2 = context.getBean(Config2.class);
		assertThat(config2, notNullValue());
		assertThat(config2.zookeeperLeaderCandidate, notNullValue());		
		assertThat(config2.hazelcastLeaderCandidate, notNullValue());
		assertThat(config2.etcdLeaderCandidate, notNullValue());
	}
	
	static class Config1 {
		
		@Autowired
		Candidate candidate;
	}

	static class Config2 {
		
		@Autowired
		@Qualifier("zookeeperLeaderCandidate")
		Candidate zookeeperLeaderCandidate;
		
		@Autowired
		@Qualifier("hazelcastLeaderCandidate")
		Candidate hazelcastLeaderCandidate;
		
		@Autowired
		@Qualifier("etcdLeaderCandidate")
		Candidate etcdLeaderCandidate;
		
	}
	
}
