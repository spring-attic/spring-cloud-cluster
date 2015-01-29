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
package org.springframework.cloud.cluster.autoconfigure.leader;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Shared stuff for leader auto-configuration tests.
 * 
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractLeaderAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context;
	
	protected ZookeeperTestingServerWrapper zookeeper;

	@Before
	public void setup() throws Exception {
		zookeeper = setupZookeeperTestingServer();
		context = setupContext();
	}
	
	@After
	public void close() {
		if (context != null) {
			context.close();
		}
		if (zookeeper != null) {
			try {
				zookeeper.destroy();
			} catch (Exception e) {
			}
			zookeeper = null;
		}
	}
	
	protected ZookeeperTestingServerWrapper setupZookeeperTestingServer() throws Exception {
		return null;
	}
	
	protected AnnotationConfigApplicationContext setupContext() {
		return new AnnotationConfigApplicationContext();
	}

	static class ZookeeperTestingServerWrapper implements DisposableBean {

		TestingServer testingServer;

		public ZookeeperTestingServerWrapper() throws Exception {
			this.testingServer = new TestingServer(true);
		}

		@Override
		public void destroy() throws Exception {
			try {
				testingServer.close();
			}
			catch (IOException e) {
			}
		}

		public int getPort() {
			return testingServer.getPort();
		}

	}
	
}
