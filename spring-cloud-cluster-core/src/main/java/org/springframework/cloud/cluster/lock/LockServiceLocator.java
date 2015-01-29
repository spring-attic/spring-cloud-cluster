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
package org.springframework.cloud.cluster.lock;

/**
 * Lock service locator defines a contract matching a lock
 * key to a {@link LockService}.
 * 
 * <p>Implementation is free to implement this interface as
 * it wish. Possible implementation can i.e. choose to locate
 * different services based on path matching or any other means
 * which creates a consistent resolving to a service.
 * 
 * @author Janne Valkealahti
 *
 */
public interface LockServiceLocator {

	/**
	 * Locates a bound {@link LockService} for a locking key.
	 * 
	 * <p>Locate algorithm has to be consistent among different
	 * JVM's meaning that in all cases a locking key has to resolve
	 * back to same {@link LockService} where {@link DistributedLock}
	 * either already exists or will exist once it is created.
	 * 
	 * @param lockKey the locking key
	 * @return lock service or null if key is not bound to any service
	 */
	LockService locate(String lockKey);
	
}
