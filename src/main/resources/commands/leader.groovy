package commands

import org.crsh.cli.Usage
import org.crsh.cli.Command
import org.crsh.command.InvocationContext

/**
 * Custom CRaSSH command for interacting with the Curator/ZooKeeper
 * leadership utility in the sample app
 * {@link org.springframework.cloud.cluster.zk.leader.ZKTestApplication}.
 */
@Usage("Leadership commands")
class leader {

	@Usage("Resign leadership")
	@Command
	def resign(InvocationContext context) {
		def factory = context.attributes["spring.beanfactory"]
		def candidate = factory.getBean("candidate")

		def leaderContext = candidate.leaderContext;
		if (leaderContext == null) {
			context.writer.println(sprintf("candidate %s is not leader", candidate.getId()))
		}
		else {
			candidate.leaderContext.renounce()
			context.writer.println(sprintf("candidate %s has renounced leadership", candidate.getId()))
		}
	}

	@Usage("Start leadership candidacy")
	@Command
	def start(InvocationContext context) {
		def factory = context.attributes["spring.beanfactory"]
		def initiator = factory.getBean("initiator")

		initiator.start()
	}

	@Usage("Stop leadership candidacy")
	@Command
	def stop(InvocationContext context) {
		def factory = context.attributes["spring.beanfactory"]
		def initiator = factory.getBean("initiator")

		initiator.stop()
	}

}