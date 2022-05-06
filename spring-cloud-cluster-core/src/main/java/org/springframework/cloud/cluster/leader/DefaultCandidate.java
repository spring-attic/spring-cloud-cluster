/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.cloud.cluster.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link org.springframework.cloud.cluster.leader.Candidate} for leadership.
 * This implementation simply logs when it is elected and when its leadership is revoked.
 * @deprecated in favour of equivalent functionality in Spring Integration 4.3
 */
@Deprecated
public class DefaultCandidate extends AbstractCandidate {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private volatile Context leaderContext;

	/**
	 * Instantiate a default candidate.
	 */
	public DefaultCandidate() {
		super();
	}

	/**
	 * Instantiate a default candidate.
	 *
	 * @param id the identifier
	 * @param role the role
	 */
	public DefaultCandidate(String id, String role) {
		super(id, role);
	}

	@Override
	public void onGranted(Context ctx) {
		this.logger.info("{} has been granted leadership; context: {}", this, ctx);
		this.leaderContext = ctx;
	}

	@Override
	public void onRevoked(Context ctx) {
		this.logger.info("{} leadership has been revoked", this, ctx);
	}

	/**
	 * Voluntarily yield leadership if held. If leader context is not
	 * yet known this method does nothing. Leader context becomes available
	 * only after {@link #onGranted(Context)} method is called by the
	 * leader initiator.
	 */
	public void yieldLeadership() {
		if (this.leaderContext != null) {
			this.leaderContext.yield();
		}
	}

	@Override
	public String toString() {
		return String.format("DefaultCandidate{role=%s, id=%s}", getRole(), getId());
	}

}
