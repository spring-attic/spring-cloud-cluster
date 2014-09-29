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

package org.springframework.cloud.cluster.hazelcast.leader;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.test.SimpleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Sample Spring Boot application that demonstrates leader election
 * with the Spring Cloud leadership API using Hazelcast
 * as the underlying implementation.
 *
 * @author Patrick Peralta
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class HazelcastTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(HazelcastTestApplication.class, args);
	}

	@Bean
	public Candidate candidate() {
		return new SimpleCandidate();
	}

	@Bean
	public HazelcastInstance hazelcastInstance() {
		return Hazelcast.newHazelcastInstance();
	}

	@Bean
	public LeaderInitiator initiator() {
		return new LeaderInitiator(hazelcastInstance(), candidate());
	}
}
