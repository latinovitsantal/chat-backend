package hu.latantal.chat

import hu.latantal.chat.neo.*
import hu.latantal.chat.service.*
import org.springframework.boot.*
import org.springframework.boot.autoconfigure.*
import org.springframework.context.annotation.*

fun main(args: Array<String>) {
	runApplication<ChatApplication>(*args)
}

@SpringBootApplication
class ChatApplication(
		val u: UserService,
		val c: ContactService,
		val m: MessageService,
		val g : GroupService
) {

	@Bean
	fun init() = CommandLineRunner {

		Neo.deleteAllNodes()

		val alice = "alice"
		val bob = "bob"
		val anthony = "anthony"
		val monica = "monica"
		val mano = "mano"
		val pass = "pass"

		u.register(alice, pass)
		u.register(bob, pass)
		u.register(anthony, pass)
		u.register(monica, pass)
		u.register(mano, pass)
		u.refreshPassword(monica, mano)

		u.register("toDelete", pass)
		u.deleteByName("toDelete")

		c.sendRequest(alice, bob)
		c.sendRequest(alice, mano)
		c.sendRequest(bob, mano)
		c.sendRequest(anthony, monica)
		c.sendRequest(alice, anthony)
		c.sendRequest(monica, mano)
		c.sendRequest(bob, monica)

		c.acceptRequest(alice, anthony)
		c.acceptRequest(anthony, monica)
		c.acceptRequest(alice, bob)
		c.declineRequest(alice, mano)

		g.createPrivateGroup(monica, "our group")
		var id = g.getGroupsOf(monica).first().id
		g.sendInvitation(monica, anthony, id)
		g.sendInvitation(monica, mano, id)
		g.sendInvitation(monica, bob, id)
		id = g.getInvitations(anthony).first().groupId
		g.acceptInvitation(anthony, id)
		g.declineInvitation(bob, id)

		g.createPublicGroup(bob, "bobgroup")
		g.createPublicGroup(bob, "bobgroup")

		id = g.getPublicGroupsByNameContaining("bgr", 20)[0].id
		(1..30).forEach { m.send(bob, id, "bobgroup echo $it") }

		println("alice's conversations:")
		id = c.getContactsOf("alice").findLast { it.username == bob }!!.id
		for (i in 1..30) {
			m.send(alice, id, "hi $i")
			m.send(bob, id, "hy $i")
		}
		c.getContactsOf("alice").forEach {
			val ms = m.getMessages(alice, it.id, 3)
			println(it.username)
			println("\tlast 3:")
			ms.forEach { println("\t\t$it") }
			println("\tthe rest:")
			m.getPrevMessages(alice, it.id, ms.last().id, 20).forEach { println("\t\t$it") }
		}

		println("\nmano's received request's:")
		c.getRequestsReceived(mano).forEach {
			println("\t$it")
		}

		println("\nbob's sent requests:")
		c.getRequestsSent(bob).forEach {
			println("\t$it")
		}

		println("\nis name 'anti' used: ${u.isNameUsed("anti")}")
		println("is name 'anthony' used: ${u.isNameUsed("anthony")}")

	}

}


