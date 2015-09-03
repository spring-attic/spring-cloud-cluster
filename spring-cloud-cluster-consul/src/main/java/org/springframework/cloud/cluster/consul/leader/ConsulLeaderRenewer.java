package org.springframework.cloud.cluster.consul.leader;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Spencer Gibb
 */
public class ConsulLeaderRenewer {

	private final ConsulLeaderInitiator initiator;

	public ConsulLeaderRenewer(ConsulLeaderInitiator initiator) {
		this.initiator = initiator;
	}

	@Scheduled(fixedDelayString = "${spring.cloud.cluster.consul.leader.session.renewalDelay}")
	public void renewSession() {
		initiator.renew();
	}
}
