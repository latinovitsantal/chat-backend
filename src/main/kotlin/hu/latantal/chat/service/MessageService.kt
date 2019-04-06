package hu.latantal.chat.service

import hu.latantal.chat.data.*
import hu.latantal.chat.neo.*
import org.springframework.stereotype.*

@Service
class MessageService {

	fun send(sender: String, convoId: Long, text: String): Message? {
		return let(sender be "sender", convoId be "convoId", text be "text", now be "time").inQuery(
				"""
					MATCH (a:User{name:{sender}})-[ca:HAS_CONTACT|MEMBER_OF]->(c) WHERE id(c) = {convoId}
					SET c.lock = TRUE, c.messageCount = c.messageCount+1, ca.seen = c.messageCount
					WITH c, a
					MATCH (c)-[r:HAS_PREV_MESSAGE]->(m0:Message)
					DELETE r
					CREATE
						(c)-[:HAS_PREV_MESSAGE]->
						(m1:Message{text:{text},time:{time}})
						-[:HAS_PREV_MESSAGE]->(m0),
						(c)-[:HAS_MESSAGE]->(m1),
						(a)-[:SENT]->(m1)
					SET c.lastUpdated = {time}
					REMOVE c.lock
					RETURN m1, id(m1) as id
				"""
		).map {
			it["m1"].convertTo<Message>().apply {
				username = sender
				id = it["id"].long
			}
		}.firstOrNull()
	}

	fun getMessages(name: String, convoId: Long, limit: Int): List<Message> {
		return let(name be "name", convoId be "convoId").inQuery(
				"""
						MATCH (a:User{name:{name}})-[:HAS_CONTACT|MEMBER_OF]->(c) WHERE id(c) = {convoId}
						MATCH (c)-[:HAS_PREV_MESSAGE*1..$limit]->(m:Message)
						OPTIONAL MATCH (m)<-[:SENT]-(u:User)
						RETURN m, u.name AS n, id(m) AS id
				"""
		).map {
			it["m"].convertTo<Message>().apply {
				username = it["n"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

	fun getPrevMessages(name: String, convoId: Long, messageId: Long, limit: Int): List<Message> {
		return let(name be "name", messageId be "messageId", convoId be "convoId").inQuery(
				"""
					MATCH (:User{name:{name}})-[:HAS_CONTACT|MEMBER_OF]->(c) WHERE id(c) = {convoId}
					MATCH (c)-[:HAS_MESSAGE]->(m:Message) WHERE id(m) = {messageId}
					MATCH (m)-[:HAS_PREV_MESSAGE*1..$limit]->(p)
					OPTIONAL MATCH (u:User)-[:SENT]->(p)
					RETURN u.name AS n, p, id(p) AS id
				"""
		).map {
			it["p"].convertTo<Message>().apply {
				username = it["n"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

	fun see(name: String, convoId: Long, lastSeenId: Long): List<Message> {
		return let(name be "name", convoId be "convoId", lastSeenId be "lastSeenId").inQuery(
				"""
					MATCH (:User{name:{name}})-[r:HAS_CONTACT|MEMBER_OF]->(c) WHERE id(c) = {convoId}
					MATCH (c)-[:HAS_MESSAGE]->(m:Message)<-[:HAS_PREV_MESSAGE*]-(n:Message) WHERE id(m) = {lastSeenId}
					OPTIONAL MATCH (u:User)-[:SENT]->(n)
					SET r.seen = c.messageCount
					RETURN n AS new, id(n) AS id, u.name AS name
				"""
		).map {
			it["new"].convertTo<Message>().apply {
				username = it["name"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

	fun getMessagesOfPublicGroup(groupId: Long, limit: Int): List<Message> {
		return let(groupId be "id").inQuery(
				"""
						MATCH (g:Group) WHERE id(g) = {id}
						MATCH (g)-[:HAS_PREV_MESSAGE*1..$limit]->(m:Message)
						OPTIONAL MATCH (m)<-[:SENT]-(u:User)
						RETURN m, u.name AS n, id(m) AS id
				"""
		).map {
			it["m"].convertTo<Message>().apply {
				username = it["n"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

	fun getPrevMessagesOfPublicGroup(groupId: Long, messageId: Long, limit: Int): List<Message> {
		return let(groupId be "id", messageId be "mid").inQuery(
				"""
					MATCH (g:Group) WHERE id(g) = {id}
					MATCH (g)-[:HAS_MESSAGE]->(m:Message) WHERE id(m) = {mid}
					MATCH (m)-[:HAS_PREV_MESSAGE*1..$limit]->(p)
					OPTIONAL MATCH (u:User)-[:SENT]->(p)
					RETURN u.name AS n, p, id(p) AS id
				"""
		).map {
			it["p"].convertTo<Message>().apply {
				username = it["n"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

	fun getNewMessagesOfPublicGroup(groupId: Long, lastSeenId: Long): List<Message> {
		return let(groupId be "groupId", lastSeenId be "lastSeenId").inQuery(
				"""
					MATCH (g:Group) WHERE id(g) = {groupId}
					MATCH (g)-[:HAS_MESSAGE]->(m:Message)<-[:HAS_PREV_MESSAGE*]-(n:Message)
						WHERE id(m) = {lastSeenId}
					OPTIONAL MATCH (u:User)-[:SENT]->(n)
					RETURN n AS new, id(n) AS id, u.name AS name
				"""
		).map {
			it["new"].convertTo<Message>().apply {
				username = it["name"].stringOrNull
				id = it["id"].long
			}
		}.toList()
	}

}