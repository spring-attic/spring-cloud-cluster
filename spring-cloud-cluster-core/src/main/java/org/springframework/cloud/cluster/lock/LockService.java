/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.cluster.lock;

/**
 * {@code LockService} implementations provide low
 * level access to a particular locking service. This
 * service is always backed by a real locking service
 * bound to a real distributed system like zookeeper
 * or redis.
 *
 * @author Janne Valkealahti
 *
 * @deprecated in favour of equivalent functionality in Spring Integration 4.3
 */
@Deprecated
public interface LockService {

	/**
	 * Obtains a {@link DistributedLock} from a service. Obtaining
	 * a lock should not do any locking operations like trying
	 * a lock. All possible locking actions should be left to a user
	 * to be executed via {@link DistributedLock}.
	 *
	 * @param lockKey the locking key
	 * @return distributed lock
	 */
	DistributedLock obtain(String lockKey);

}
