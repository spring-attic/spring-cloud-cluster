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

/**
 * Base implementation of {@link DistributedLock}.
 * 
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractDistributedLock implements DistributedLock {

	private final String lockKey;
	
	/**
	 * Instantiates a new abstract distributed lock.
	 *
	 * @param lockKey the locking key
	 */
	public AbstractDistributedLock(String lockKey) {
		this.lockKey = lockKey;
	}

	@Override
	public String getLockKey() {
		return lockKey;
	}

}
