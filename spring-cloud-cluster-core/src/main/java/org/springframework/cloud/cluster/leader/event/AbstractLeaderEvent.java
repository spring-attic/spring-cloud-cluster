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

import org.springframework.cloud.cluster.leader.Context;
import org.springframework.context.ApplicationEvent;

/**
 * Base {@link ApplicationEvent} class for leader based events. All custom event
 * classes should be derived from this class.
 *
 * @author Janne Valkealahti
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractLeaderEvent extends ApplicationEvent {

	private Context context;
	
	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the component that published the event (never {@code null})
	 */
	public AbstractLeaderEvent(Object source) {
		super(source);
	}

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 */
	public AbstractLeaderEvent(Object source, Context context) {
		super(source);
		this.context = context;
	}

	/**
	 * Gets the {@link Context} associated with this event.
	 * 
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}

	@Override
	public String toString() {
		return "AbstractLeaderEvent [context=" + context + ", source=" + source
				+ "]";
	}

}
