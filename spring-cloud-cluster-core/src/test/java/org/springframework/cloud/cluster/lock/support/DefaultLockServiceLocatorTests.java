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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.cloud.cluster.TestUtils;
import org.springframework.cloud.cluster.lock.AbstractLockingTests;
import org.springframework.cloud.cluster.lock.LockService;

/**
 * Tests for {@link DefaultLockServiceLocator}.
 * 
 * @author Janne Valkealahti
 *
 */
public class DefaultLockServiceLocatorTests extends AbstractLockingTests {

	@Test
	public void testPathMatching() throws Exception {
		DefaultLockServiceLocator locator = new DefaultLockServiceLocator(
				new LockService1());
		locator.addMapping("/path2/**", new LockService2());
		locator.addMapping("/path3/**", new LockService3());

		LockService matched = match(locator, "/path2/dlock1");
		assertThat(matched, instanceOf(LockService2.class));
		
		matched = match(locator, "/path3/dlock1");
		assertThat(matched, instanceOf(LockService3.class));
		
		matched = match(locator, "/path1/dlock1");
		assertThat(matched, nullValue());
	}

	@Test
	public void testLockServiceLocate() throws Exception {
		DefaultLockServiceLocator locator = new DefaultLockServiceLocator(
				new LockService1());
		locator.addMapping("/path2/**", new LockService2());
		locator.addMapping("/path3/**", new LockService3());
		
		LockService service = locator.locate("/path1/dlock1");
		assertThat(service, instanceOf(LockService1.class));

		service = locator.locate("xxx");
		assertThat(service, instanceOf(LockService1.class));
		
		service = locator.locate("/path2/dlock1");
		assertThat(service, instanceOf(LockService2.class));
		
		service = locator.locate("/path3/dlock1");
		assertThat(service, instanceOf(LockService3.class));
	}
	
	private static LockService match(Object locator, String key)
			throws Exception {
		return TestUtils.callMethod("match", locator, new String[] { key },
				new Class[] { String.class });
	}
	
}
