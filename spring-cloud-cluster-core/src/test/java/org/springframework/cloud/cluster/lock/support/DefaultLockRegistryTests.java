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

import org.junit.Test;
import org.springframework.cloud.cluster.lock.LockService;
import org.springframework.cloud.cluster.lock.LockServiceLocator;
import org.springframework.cloud.cluster.lock.LockingException;

/**
 * Tests for {@link DefaultLockRegistry}.
 * 
 * @author Janne Valkealahti
 *
 */
public class DefaultLockRegistryTests {

	@Test(expected = LockingException.class)
	public void testFailureWithMissingLocate() {
		DefaultLockRegistry registry = new DefaultLockRegistry(new AlwaysNullLockServiceLocator());
		registry.get("cantfind");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailureWithNullLockService() {
		new DefaultLockRegistry(null);
	}	

	private static class AlwaysNullLockServiceLocator implements LockServiceLocator {

		@Override
		public LockService locate(String lockKey) {
			return null;
		}
		
	}

}
