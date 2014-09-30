/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.cloud.cluster.test;

import org.springframework.boot.SpringApplication;

/**
 * Launcher for the various example applications for leader election. Usage: run with
 * <code>--type=zk</code> or <code>--type=hz</code> for Zookeeper or Hazelcast.
 *
 * @author Patrick Peralta
 * @author Dave Syer
 */
public class Application {
	public static void main(String[] args) throws Exception {
		SpringApplication.main(args);
	}
}
