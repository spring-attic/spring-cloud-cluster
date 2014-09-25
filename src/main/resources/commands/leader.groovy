package commands

import org.crsh.cli.Usage
import org.crsh.cli.Command
import org.crsh.command.InvocationContext

@Usage("Leadership commands")
class leader {

	@Usage("Resign leadership")
	@Command
	def resign(InvocationContext context) {
		def factory = context.attributes["spring.beanfactory"]
		def candidate = factory.getBean("candidate")

		candidate.leaderContext.renounce()
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