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
package org.springframework.cloud.cluster.lock.support;

import org.springframework.cloud.cluster.lock.DistributedLock;
import org.springframework.cloud.cluster.lock.LockRegistry;
import org.springframework.cloud.cluster.lock.LockService;
import org.springframework.cloud.cluster.lock.LockServiceLocator;
import org.springframework.cloud.cluster.lock.LockingException;
import org.springframework.util.Assert;

/**
 * Default implementation of a {@link LockRegistry} delegating
 * to a {@link LockServiceLocator}.
 * 
 * @author Janne Valkealahti
 *
 */
public class DefaultLockRegistry implements LockRegistry {

	private final LockServiceLocator lockServiceLocator;

	/**
	 * Instantiates a new default lock registry.
	 *
	 * @param lockServiceLocator the lock service locator
	 */
	public DefaultLockRegistry(LockServiceLocator lockServiceLocator) {
		Assert.notNull(lockServiceLocator, "Lock service locator must be set");
		this.lockServiceLocator = lockServiceLocator;
	}

	@Override
	public DistributedLock get(String lockKey) {
		LockService service = lockServiceLocator.locate(lockKey);
		if (service == null) {
			throw new LockingException("Unable to find lockservice for key=["
					+ lockKey + "]");
		}
		return service.obtain(lockKey);
	}

}
