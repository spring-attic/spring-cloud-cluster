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
package org.springframework.cloud.cluster.redis.lock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cloud.cluster.lock.DistributedLock;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Integration tests for redis locking using external redis server.
 * 
 * @author Janne Valkealahti
 *
 */
public class RedisIT {

	private AnnotationConfigApplicationContext context;
	private RedisConnectionFactory connectionFactory;
	private RedisTemplate<String, String> redisTemplate;

	@Before
	public void setup() {
		context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context);
		context.register(RedisAutoConfiguration.class);
		context.refresh();
		connectionFactory = context.getBean(RedisConnectionFactory.class);
		redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.afterPropertiesSet();
		cleanLocks();
	}
	
	@After
	public void close() {
		cleanLocks();
		context.close();
	}
	
	private void cleanLocks() {
		Set<String> keys = redisTemplate.keys(RedisLockService.DEFAULT_REGISTRY_KEY + ":*");
		redisTemplate.delete(keys);
	}
	
	@Test
	public void testSimpleLock() {
		RedisLockService lockService = new RedisLockService(connectionFactory);
		DistributedLock lock = lockService.obtain("lock");
		lock.lock();

		Set<String> keys = redisTemplate.keys(RedisLockService.DEFAULT_REGISTRY_KEY + ":*");
		assertThat(keys.size(), is(1));
		assertThat(keys.iterator().next(), is(RedisLockService.DEFAULT_REGISTRY_KEY + ":lock"));
		
		lock.unlock();
	}

	@Test
	public void testSecondLockSucceed() {
		RedisLockService lockService = new RedisLockService(connectionFactory);
		DistributedLock lock1 = lockService.obtain("lock");
		DistributedLock lock2 = lockService.obtain("lock");
		lock1.lock();
		// same thread so try/lock doesn't fail
		assertThat(lock2.tryLock(), is(true));
		lock2.lock();

		Set<String> keys = redisTemplate.keys(RedisLockService.DEFAULT_REGISTRY_KEY + ":*");
		assertThat(keys.size(), is(1));
		assertThat(keys.iterator().next(), is(RedisLockService.DEFAULT_REGISTRY_KEY + ":lock"));
		
		lock1.unlock();
		lock2.unlock();
	}
	
}
