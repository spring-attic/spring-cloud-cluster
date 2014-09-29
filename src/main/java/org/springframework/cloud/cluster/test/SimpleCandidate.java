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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;

/**
 * Simple {@link org.springframework.cloud.cluster.leader.Candidate} for leadership. This implementation simply
 * logs when it is elected and when its leadership is revoked.
 */
public class SimpleCandidate implements Candidate {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String ROLE = "leader";

	private final String id = UUID.randomUUID().toString();

	private volatile Context leaderContext;

	@Override
	public String getRole() {
		return ROLE;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void onGranted(Context ctx) throws InterruptedException {
		logger.info("{} has been granted leadership; context: {}", this, ctx);
		leaderContext = ctx;
	}

	@Override
	public void onRevoked(Context ctx) {
		logger.info("{} leadership has been revoked", this, ctx);
	}

	public Context getLeaderContext() {
		// TODO: this is being exposed so that the CRaSSH command
		// can access the context to cancel leadership; should this
		// be exposed to the Candidate interface? Or should the interface
		// itself include a "renounce" method?
		return leaderContext;
	}

	@Override
	public String toString() {
		return String.format("SimpleCandidate{role=%s, id=%s}", getRole(), getId());
	}

}
