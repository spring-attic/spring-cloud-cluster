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
import org.springframework.cloud.cluster.hazelcast.leader.HazelcastTestApplication;
import org.springframework.cloud.cluster.zk.leader.ZKTestApplication;

/**
 * Launcher for the various example applications for leader election.
 *
 * @author Patrick Peralta
 */
public class Application {
	public static void main(String[] args) {
		Class clz;
		String arg0 = null;

		if (args.length > 0) {
			arg0 = args[0];
		}

		if ("zk".equalsIgnoreCase(arg0)) {
			clz = ZKTestApplication.class;
		}
		else if ("hz".equalsIgnoreCase(arg0)) {
			clz = HazelcastTestApplication.class;
		}
		else {
			System.out.println("Usage: Application [ zk | hz ]");
			System.out.println("  zk for ZooKeeper");
			System.out.println("  hz for Hazelcast");
			return;
		}

		SpringApplication.run(clz, args);
	}
}
