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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.cluster.lock.DistributedLockProperties;
import org.springframework.cloud.cluster.redis.RedisClusterProperties;
import org.springframework.cloud.cluster.redis.lock.RedisLockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Auto-configuration for {@link RedisLockService}.
 * 
 * @author Janne Valkealahti
 *
 */
@Configuration
@ConditionalOnClass(RedisLockService.class)
@ConditionalOnProperty(value = { "spring.cloud.cluster.redis.lock.enabled",
		"spring.cloud.cluster.lock.enabled" }, matchIfMissing = true)
@EnableConfigurationProperties({ DistributedLockProperties.class,
		RedisClusterProperties.class })
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisLockServiceAutoConfiguration {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;
	
	@Autowired
	private DistributedLockProperties distributedLockProperties;
	
	@Autowired
	private RedisClusterProperties redisClusterProperties;
	
	@Bean
	public RedisLockService redisLockService() {
		String role = distributedLockProperties.getRole();
		Long expire = redisClusterProperties.getLock().getExpireAfter();				
		if (role != null && expire != null) {
			return new RedisLockService(redisConnectionFactory, role, expire);
		} else if (role != null) {
			return new RedisLockService(redisConnectionFactory, role);
		} else if (expire != null) {
			return new RedisLockService(redisConnectionFactory, expire);
		} else {
			return new RedisLockService(redisConnectionFactory);
		}
	}

}
