/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.cloud.cluster.consul.leader;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.cluster.consul.ConsulClusterProperties;
import org.springframework.cloud.cluster.leader.Candidate;
import org.springframework.cloud.cluster.leader.Context;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisher;
import org.springframework.context.Lifecycle;
import org.springframework.util.StringUtils;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.ecwid.consul.v1.session.model.NewSession;

/**
 * Bootstrap leadership {@link org.springframework.cloud.cluster.leader.Candidate candidates}
 * with Consul. Upon construction, {@link #start} must be invoked to
 * register the candidate for leadership election.
 *
 * https://consul.io/docs/guides/leader-election.html
 * https://consul.io/docs/agent/http/session.html
 * https://consul.io/docs/internals/sessions.html
 *
 * @author Patrick Peralta
 * @author Janne Valkealahti
 * @author Spencer Gibb
 *
 */
public class ConsulLeaderInitiator implements Lifecycle, InitializingBean, DisposableBean {

	public static final int MAX_WAIT_TIME_SECONDS = 600;
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Consul client.
	 */
	private final ConsulClient client;

	/**
	 * Candidate for leader election.
	 */
	private final Candidate candidate;

	/**
	 * Executor service for running leadership daemon.
	 */
	//TODO: allow user to set?
	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Consul leadership");
			thread.setDaemon(true);
			return thread;
		}
	});

	private final ConsulClusterProperties.LeaderProperties properties;

	/**
	 * Future returned by submitting an {@link Initiator} to {@link #executorService}.
	 * This is used to cancel leadership.
	 */
	private volatile Future<Void> future;

	/**
	 * If the candidate is leader
	 */
	private volatile AtomicBoolean leader = new AtomicBoolean(false);

	/**
	 * If the candidate is leader
	 */
	private volatile AtomicReference<String> sessionId = new AtomicReference<>(null);

	/**
	 * Flag that indicates whether the leadership election for
	 * this {@link #candidate} is running.
	 */
	private volatile AtomicBoolean running = new AtomicBoolean(false);

	/** Leader event publisher if set */
	private volatile LeaderEventPublisher leaderEventPublisher;

	/**
	 * Construct a {@link ConsulLeaderInitiator}.
	 *
	 * @param client     Consul client
	 * @param candidate  leadership election candidate
	 * @param properties  Consul cluster properties
	 */
	public ConsulLeaderInitiator(ConsulClient client, Candidate candidate, ConsulClusterProperties properties) {
		this.client = client;
		this.candidate = candidate;
		this.properties = properties.getLeader();
	}
	
	/**
	 * Start the registration of the {@link #candidate} for leader election.
	 */
	@Override
	public synchronized void start() {
		if (running.compareAndSet(false, true)) {
			future = executorService.submit(new Initiator());
		}
	}

	/**
	 * Stop the registration of the {@link #candidate} for leader election.
	 * If the candidate is currently leader, its leadership will be revoked.
	 */
	@Override
	public synchronized void stop() {
		if (running.compareAndSet(true, false)) {
			future.cancel(true);
		}
	}

	public boolean renew() {
		if (running.get() && sessionId.get() != null) {
			try {
				client.renewSession(sessionId.get(), QueryParams.DEFAULT);
				return true;
			} catch (OperationException e) {
				log.warn(String.format("Unable to renew session: %s, statusCode: %s, statusMsg: %s",
						sessionId.get(), e.getStatusCode(), e.getStatusMessage()), e);
			}
		}
		return false;
	}

	/**
	 * @return true if leadership election for this {@link #candidate} is running
	 */
	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	@Override
	public void destroy() throws Exception {
		stop();
		executorService.shutdown();
	}

	/**
	 * Sets the {@link LeaderEventPublisher}.
	 * 
	 * @param leaderEventPublisher the event publisher
	 */
	public void setLeaderEventPublisher(LeaderEventPublisher leaderEventPublisher) {
		this.leaderEventPublisher = leaderEventPublisher;
	}

	/**
	 * Callable that manages the acquisition of Consul locks
	 * for leadership election.
	 */
	class Initiator implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			ConsulContext context = new ConsulContext();
			long index = -1;
			boolean locked = false;

			if (!StringUtils.hasText(sessionId.get())) {
				NewSession session = new NewSession();
				session.setName(candidate.getRole());
				session.setTtl(properties.getSession().getTtl());
				session.setBehavior(properties.getSession().getBehavior());
				session.setLockDelay(properties.getSession().getLockDelay());
				//TODO: if TTL is not null, sessions would need to be renewed
				//TODO: checks
				Response<String> sessionResp = client.sessionCreate(session, QueryParams.DEFAULT);
				sessionId.set(sessionResp.getValue());
			}

			while (running.get()) {
				try {
					QueryParams queryParams;
					if (index == -1) {
						queryParams = QueryParams.DEFAULT;
					} else {
						//block
						queryParams = new QueryParams(MAX_WAIT_TIME_SECONDS, index);
					}

					boolean keyHasSession = false;
					try {
						Response<GetValue> response = client.getKVValue(buildLeaderKey(), queryParams);
						index = response.getConsulIndex();
						keyHasSession = response.getValue() != null && response.getValue().getSession() == null;
					} catch (OperationException e) {
						if (e.getStatusCode() == 404) {
							keyHasSession = false;
						} else {
							throw e;
						}
					}

					if (!keyHasSession) {
						// there is no lock
						PutParams params = new PutParams();
						params.setAcquireSession(sessionId.get());
						Response<Boolean> lockResp = client.setKVValue(buildLeaderKey(), candidate.getId(), params);
						locked = lockResp.getValue();
						if (locked) {
							leader.set(true);
							candidate.onGranted(context);
							if (leaderEventPublisher != null) {
								leaderEventPublisher.publishOnGranted(ConsulLeaderInitiator.this, context);
							}
							Thread.sleep(Long.MAX_VALUE);
						}
					}
				}
				catch (InterruptedException e) {
					// InterruptedException, like any other runtime exception,
					// is handled by the finally block below. No need to
					// reset the interrupt flag as the interrupt is handled.
				}
				catch (OperationException e) {
					log.warn(String.format("Error trying to become leader: %s, statusCode: %s, statusMsg: %s",
							sessionId.get(), e.getStatusCode(), e.getStatusMessage()), e);
				}
				finally {
					if (locked) {
						leader.set(false);
						candidate.onRevoked(context);
						if (leaderEventPublisher != null) {
							leaderEventPublisher.publishOnRevoked(ConsulLeaderInitiator.this, context);
						}
						locked = false;
					}
				}
			}

			return null;
		}
	}

	/**
	 * @return the Consul key used for leadership election by Consul
	 */
	private String buildLeaderKey() {

		String ns = properties.getNamespace();
		if (ns.startsWith("/")) {
			ns = ns.substring(1);
		}
		return ns + candidate.getRole();
	}

	/**
	 * Implementation of leadership context backed by Consul.
	 */
	class ConsulContext implements Context {

		@Override
		public boolean isLeader() {
			return leader.get();
		}

		@Override
		public void yield() {
			if (future != null) {
				future.cancel(true);
			}
			if (sessionId.get() != null) {
				PutParams params = new PutParams();
				params.setReleaseSession(sessionId.get());
				client.setKVValue(buildLeaderKey(), candidate.getId(), params);
			}
		}

		public String getSessionId() {
			return sessionId.get();
		}

		@Override
		public String toString() {
			return String.format("ConsulContext{role=%s, id=%s, isLeader=%s, sessionId=%s}",
					candidate.getRole(), candidate.getId(), isLeader(), sessionId.get());
		}

	}
}
