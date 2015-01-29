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

import java.util.ArrayList;

import org.springframework.cloud.cluster.lock.LockService;
import org.springframework.cloud.cluster.lock.LockServiceLocator;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;

/**
 * Default implementation of a {@link LockServiceLocator} which
 * uses a set of {@link LockService}s where matching happens using
 * a simple {@link PathMatcher}.
 * 
 * @author Janne Valkealahti
 *
 */
public class DefaultLockServiceLocator implements LockServiceLocator {
	
	private final ArrayList<PathMapping> mappings = new ArrayList<PathMapping>();
	
	private final PathMatcher matcher = new AntPathMatcher();
	
	private final LockService fallback;
	
	/**
	 * Instantiates a new default lock service locator.
	 *
	 * @param fallback the primary lock service
	 */
	public DefaultLockServiceLocator(LockService fallback) {
		Assert.notNull(fallback, "Fallback lock service must be set");
		this.fallback = fallback;
	}

	@Override
	public LockService locate(String lockKey) {
		LockService match = match(lockKey);
		return match != null ? match : fallback;
	}
	
	/**
	 * Adds a mapping path for lock service.
	 *
	 * @param path the path
	 * @param lockService the lock service
	 */
	public void addMapping(String path, LockService lockService) {
		Assert.notNull(lockService, "Lock service must not be null");
		mappings.add(new PathMapping(path, lockService));
	}
	
	private LockService match(String path) {
		for (PathMapping m : mappings) {
			if (matcher.match(m.getPath(), path)) {
				return m.getLockService();
			}
		}
		return null;
	}
	
	private static class PathMapping {
		
		private String path;
		
		private LockService lockService;
		
		public PathMapping(String path, LockService lockService) {
			this.path = path;
			this.lockService = lockService;
		}
		
		public String getPath() {
			return path;
		}
		
		public LockService getLockService() {
			return lockService;
		}
		
	}

}
