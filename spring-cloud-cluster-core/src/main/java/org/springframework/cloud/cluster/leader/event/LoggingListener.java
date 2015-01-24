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
package org.springframework.cloud.cluster.leader.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

/**
 * Simple {@link ApplicationListener} which logs all events
 * based on {@link AbstractLeaderEvent} using a log level
 * set during the construction.
 *
 * @author Janne Valkealahti
 *
 */
public class LoggingListener implements ApplicationListener<AbstractLeaderEvent> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/** Internal enums to match the log level */
	private static enum Level {
		ERROR, WARN, INFO, DEBUG, TRACE
	}

	/** Level to use */
	private final Level level;

	/**
	 * Constructs Logger listener with debug level.
	 */
	public LoggingListener() {
		level = Level.DEBUG;
	}

	/**
	 * Constructs Logger listener with given level.
	 *
	 * @param level the level string
	 */
	public LoggingListener(String level) {
		try {
			this.level = Level.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level
					+ "'. The (case-insensitive) supported values are: "
					+ StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
	}

	@Override
	public void onApplicationEvent(AbstractLeaderEvent event) {
		switch (this.level) {
		case ERROR:
			if (logger.isErrorEnabled()) {
				logger.error(event.toString());
			}
			break;
		case WARN:
			if (logger.isWarnEnabled()) {
				logger.warn(event.toString());
			}
			break;
		case INFO:
			if (logger.isInfoEnabled()) {
				logger.info(event.toString());
			}
			break;
		case DEBUG:
			if (logger.isDebugEnabled()) {
				logger.debug(event.toString());
			}
			break;
		case TRACE:
			if (logger.isTraceEnabled()) {
				logger.trace(event.toString());
			}
			break;
		}
	}

}
