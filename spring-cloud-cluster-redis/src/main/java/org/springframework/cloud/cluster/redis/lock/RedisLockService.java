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

import java.util.concurrent.locks.Lock;

import org.springframework.cloud.cluster.lock.DistributedLock;
import org.springframework.cloud.cluster.lock.LockService;
import org.springframework.cloud.cluster.lock.support.DelegatingDistributedLock;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * {@link LockService} implementation based on Redis.
 * 
 * <p>This implementation delegates to {@link RedisLockRegistry} from Spring
 * Integration.
 * 
 * @author Janne Valkealahti
 *
 */
public class RedisLockService implements LockService {
	
	public static final String DEFAULT_REGISTRY_KEY = "spring-cloud";
	
	private final RedisLockRegistry redisLockRegistry;

	/**
	 * Instantiates a new redis lock service.
	 *
	 * @param connectionFactory the redis connection factory
	 */
	public RedisLockService(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, DEFAULT_REGISTRY_KEY);
	}

	/**
	 * Instantiates a new redis lock service.
	 *
	 * @param connectionFactory the redis connection factory
	 * @param registryKey The key prefix for locks.
	 */
	public RedisLockService(RedisConnectionFactory connectionFactory, String registryKey) {
		this.redisLockRegistry = new RedisLockRegistry(connectionFactory, registryKey);
	}

	/**
	 * Instantiates a new redis lock service.
	 *
	 * @param connectionFactory the redis connection factory
	 * @param expireAfter The expiration in milliseconds.
	 */
	public RedisLockService(RedisConnectionFactory connectionFactory, long expireAfter) {
		this.redisLockRegistry = new RedisLockRegistry(connectionFactory, DEFAULT_REGISTRY_KEY, expireAfter);
	}
	
	/**
	 * Instantiates a new redis lock service.
	 *
	 * @param connectionFactory the redis connection factory
	 * @param registryKey The key prefix for locks.
	 * @param expireAfter The expiration in milliseconds.
	 */
	public RedisLockService(RedisConnectionFactory connectionFactory, String registryKey, long expireAfter) {
		this.redisLockRegistry = new RedisLockRegistry(connectionFactory, registryKey, expireAfter);
	}
	
	@Override
	public DistributedLock obtain(String lockKey) {
		Lock lock = redisLockRegistry.obtain(lockKey);
		return new DelegatingDistributedLock(lockKey, lock);
	}

}
