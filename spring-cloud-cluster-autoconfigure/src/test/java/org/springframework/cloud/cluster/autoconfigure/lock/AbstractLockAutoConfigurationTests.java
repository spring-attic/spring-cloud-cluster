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
package org.springframework.cloud.cluster.autoconfigure.lock;

import org.junit.After;
import org.junit.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Shared stuff for locking auto-configuration tests.
 * 
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractLockAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		context = setupContext();
	}
	
	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}
	
	protected AnnotationConfigApplicationContext setupContext() {
		return new AnnotationConfigApplicationContext();
	}
	
}
