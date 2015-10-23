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
package org.springframework.cloud.cluster.autoconfigure.lock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.cluster.autoconfigure.TestUtils;
import org.springframework.cloud.cluster.redis.lock.RedisLockService;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * Tests for {@link RedisLockServiceAutoConfiguration}.
 * 
 * @author Janne Valkealahti
 *
 */
public class RedisLockServiceAutoConfigurationTests extends AbstractLockAutoConfigurationTests {

	@Test
	public void testDefaults() {
		EnvironmentTestUtils.addEnvironment(this.context);
		context.register(RedisAutoConfiguration.class, RedisLockServiceAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("redisLockService"), is(true));		
	}

	@Test
	public void testDisabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.redis.lock.enabled:false");
		context.register(RedisAutoConfiguration.class, RedisLockServiceAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("redisLockService"), is(false));
	}

	@Test
	public void testGlobalLeaderDisabled() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.lock.enabled:false",
						"spring.cloud.cluster.redis.lock.enabled:true");
		context.register(RedisAutoConfiguration.class, RedisLockServiceAutoConfiguration.class);
		context.refresh();
		
		assertThat(context.containsBean("redisLockService"), is(false));
	}

	@Test
	public void testChangeRole() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.lock.role:foo");
		context.register(RedisAutoConfiguration.class, RedisLockServiceAutoConfiguration.class);
		context.refresh();
		
		RedisLockService service = context.getBean(RedisLockService.class);
		RedisLockRegistry redisLockRegistry = TestUtils.readField("redisLockRegistry", service);
		String registryKey = TestUtils.readField("registryKey", redisLockRegistry);
		
		assertThat(registryKey, is("foo"));
	}

	@Test
	public void testChangeTimeout() throws Exception {
		EnvironmentTestUtils
				.addEnvironment(
						this.context,
						"spring.cloud.cluster.redis.lock.expireAfter:1234");
		context.register(RedisAutoConfiguration.class, RedisLockServiceAutoConfiguration.class);
		context.refresh();
		
		RedisLockService service = context.getBean(RedisLockService.class);
		RedisLockRegistry redisLockRegistry = TestUtils.readField("redisLockRegistry", service);
		String registryKey = TestUtils.readField("registryKey", redisLockRegistry);
		Long expireAfter = TestUtils.readField("expireAfter", redisLockRegistry);
		
		assertThat(registryKey, is(RedisLockService.DEFAULT_REGISTRY_KEY));
		assertThat(expireAfter, is(1234l));
	}

}
