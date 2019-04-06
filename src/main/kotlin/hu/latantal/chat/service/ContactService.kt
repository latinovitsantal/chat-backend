package hu.latantal.chat.service

import hu.latantal.chat.data.*
import hu.latantal.chat.neo.*
import org.springframework.stereotype.*

@Service
class ContactService {

	fun sendRequest(sender: String, receiver: String): Boolean {
		return let(sender be "sender", receiver be "receiver", now be "time").inQuery(
				"""
					MATCH (a:User{name:{sender}})
					MATCH (b:User{name:{receiver}})
					WHERE
						NOT exists((a)-->(:Contact)<--(b))
						AND NOT exists((a)-[:REQUESTED]->(b))
					CREATE (a)-[:REQUESTED{time:{time}}]->(b)
					RETURN count(a) > 0 AS success
				"""
		)["success"].bool
	}

	fun getRequestsReceived(name: String): List<Request> {
		return let(name be "name").inQuery(
				"MATCH (:User{name:{name}})<-[r:REQUESTED]-(u:User) RETURN u.name AS n, r.time as t"
		).map { Request(it["n"].string, it["t"].string) }.toList()
	}

	fun getRequestsSent(name: String): List<Request> {
		return let(name be "name").inQuery(
				"MATCH (:User{name:{name}})-[r:REQUESTED]->(u:User) RETURN u.name AS n, r.time as t"
		).map { Request(it["n"].string, it["t"].string) }.toList()
	}

	fun acceptRequest(sender: String, receiver: String): Boolean {
		return let(sender be "sender", receiver be "receiver", now be "time").inQuery(
				"""
					MERGE (s:Sequence{name:"contactIdSequence"})
					ON CREATE SET s.current = 0
					WITH s
					MATCH p = (a:User{name:{sender}})-[r:REQUESTED]->(b:User{name:{receiver}})
					DELETE r
					FOREACH (_ IN relationships(p) |
						SET s.lock = TRUE, s.current = s.current+1
						REMOVE s.lock
					)
					CREATE
						(a)-[:HAS_CONTACT{seen:0}]->
						(c:Contact{id:s.current, messageCount:1, lastUpdated:{time}})
						<-[:HAS_CONTACT{seen:0}]-(b),
						(c)-[:HAS_PREV_MESSAGE]->(m:Message{text:"You are now connected.",time:{time}}),
						(c)-[:HAS_MESSAGE]->(m)
					RETURN count(r) > 0 AS existed
				"""
		)["existed"].bool
	}

	fun declineRequest(sender: String, receiver: String): Boolean {
		return let(sender be "sender", receiver be "receiver").inQuery(
				"""
					MATCH (:User{name:{sender}})-[r:REQUESTED]->(:User{name:{receiver}})
					DELETE r
					RETURN count(r) > 0 AS existed
				"""
		)["existed"].bool
	}

	fun deleteContact(name1: String, name2: String): Boolean {
		return let(name1 be "n1", name2 be "n2").inQuery(
				"""
					MATCH
						(a:User{name:{n1}})
						-[ra:HAS_CONTACT]->(c:Contact)<-[rb:HAS_CONTACT]-
						(b:User{name:{n2}})
					MATCH (a)-[sa:SENT]->(:Message)<-[:HAS_MESSAGE]-(c)
					MATCH (b)-[sb:SENT]->(:Message)<-[:HAS_MESSAGE]-(c)
					DELETE ra, rb, sa, sb
					RETURN count(c) > 0 AS existed
				"""
		)["existed"].bool
	}

	fun getContactsOf(name: String): List<Contact> {
		return let(name be "name").inQuery(
				"""
					MATCH (:User{name:{name}})-[hc:HAS_CONTACT]->(c:Contact)
					OPTIONAL MATCH (c)<-[:HAS_CONTACT]-(u:User) WHERE u.name <> {name}
					RETURN u.name AS n, id(c) AS id, c.messageCount - hc.seen AS unseen, c.lastUpdated AS last
				"""
		).map { Contact(it["id"].long, it["n"].stringOrNull, it["unseen"].int, it["last"].string) }.toList()
	}

}