package hu.latantal.chat.service

import hu.latantal.chat.data.*
import hu.latantal.chat.neo.*
import org.springframework.stereotype.*

@Service
class GroupService {

	fun createPrivateGroup(username: String, name: String) {
		let(username be "username", name be "name", now be "time").inQuery(
				"""
					MATCH (u:User{name:{username}})
					CREATE
						(u)-[:CREATED{time:{time}}]->
							(g:Group{name:{name}, visibility:"PRIVATE", createdAt:{time}, lastUpdated:{time}, messageCount:1})
							-[:HAS_PREV_MESSAGE]->(:Message{text:"Group created.",time:{time}}),
						(u)-[:MEMBER_OF{since:{time}, seen:0}]->(g),
						(u)-[:ADMIN_OF{since:{time}}]->(g)
				"""
		)
	}

	fun createPublicGroup(username: String, name: String): Boolean {
		return let(username be "username", name be "name", now be "time").inQuery(
				"""
					MERGE (l:Lock{name:"createPublicGroup"}) SET l.lock = TRUE
					WITH l
					MATCH (g:Group{visibility:"PUBLIC", name:{name}})
					WITH CASE WHEN count(g) = 0 THEN [1] ELSE [] END AS unused
					MATCH (u:User{name:{username}})
					FOREACH (_ IN unused |
						CREATE
							(u)-[:CREATED]->
								(g:Group{name:{name}, visibility:"PUBLIC", createdAt:{time}, lastUpdated:{time}, messageCount:1})
							-[:HAS_PREV_MESSAGE]->(:Message{text:"Group created.",time:{time}}),
							(u)-[:MEMBER_OF{since:{time}, seen:0}]->(g),
							(u)-[:ADMIN_OF{since:{time}}]->(g)
					)
					MERGE (l:Lock{name:"createPublicGroup"}) REMOVE l.lock
					RETURN size(unused) = 1 AS unused
				"""
		)["unused"].bool
	}

	fun sendInvitation(sender: String, receiver: String, groupId: Long): Boolean {
		return let(sender be "sender", receiver be "receiver", groupId be "groupId", now be "time").inQuery(
				"""
					MATCH (s:User{name:{sender}})-[:ADMIN_OF]->(g:Group) WHERE id(g) = {groupId}
					MATCH (u:User{name:{receiver}})
					WITH s, u, g,
						CASE WHEN size((u)-[:RECEIVED]->(:Invitation)<-[:HAS_INVITATION]-(g)) > 0
							THEN [] ELSE [1] END AS caseNotExists
					FOREACH (_ IN  caseNotExists |
						CREATE
							(s)-[:SENT]->(i:Invitation{time:{time}})<-[:RECEIVED]-(u),
							(g)-[:HAS_INVITATION]->(i)
					)
					RETURN size(caseNotExists) = 1 AS success
				"""
		)["success"].bool
	}

	fun acceptInvitation(receiver: String, groupId: Long): Boolean {
		return let(receiver be "receiver", groupId be "groupId").inQuery(
				"""
					MATCH (g:Group) WHERE id(g) = {groupId}
					MATCH (u:User{name:{receiver}})-[:RECEIVED]->(i:Invitation)<-[:HAS_INVITATION]-(g)
					DETACH DELETE (i)
					CREATE (u)-[:MEMBER_OF{seen:0}]->(g)
					RETURN count(i) > 0 AS existed
				"""
		)["existed"].bool
	}

	fun declineInvitation(receiver: String, groupId: Long): Boolean {
		return let(receiver be "receiver", groupId be "groupId").inQuery(
				"""
					MATCH (:User{name:{receiver}})-[:RECEIVED]->(i:Invitation)<-[:HAS_INVITATION]-(g:Group)
						WHERE id(g) = {groupId}
					DETACH DELETE i
					RETURN count(i) > 0 AS existed
				"""
		)["existed"].bool
	}

	fun getInvitations(receiver: String): List<Invitation> {
		return let(receiver be "receiver").inQuery(
			"""
				MATCH (:User{name:{receiver}})-[:RECEIVED]->(i:Invitation)<-[:HAS_INVITATION]-(g:Group)
				OPTIONAL MATCH (u:User)-[:SENT]->(i)
				RETURN id(g) AS id, g.name AS name, u.name AS sender, i.time AS time
			"""
		).map{ Invitation(it["id"].long, it["name"].string, it["sender"].stringOrNull, it["time"].string) }.toList()
	}

	fun searchPublicGroups(searchTerm: String, limit: Int): List<Group> {
		return let(searchTerm be "searchTerm", limit be "limit").inQuery(
				"""
					MATCH (g:Group{visibility:"PUBLIC"}) WHERE g.name CONTAINS {searchTerm}
					OPTIONAL MATCH (u:User)-[:CREATED]->(g)
					RETURN id(g) AS id, u.name AS u, g LIMIT {limit}
				"""
		).map{
			it["g"].convertTo<Group>().apply {
				id = it["id"].long
				creatorName = it["u"].stringOrNull
			}
		}.toList()
	}

	fun getMembersOf(username: String, id: Long): List<Member> {
		return let(username be "name", id be "id").inQuery(
				"""
					MATCH (u:User{name:{name}})-[:MEMBER_OF]->(g:Group) WHERE id(g) = {id}
					MATCH (m:User)-[:MEMBER_OF]->(g)
					RETURN m.name AS name, exists((m)-[:ADMIN_OF]->(g)) AS isAdmin
				"""
		).map { Member(it["name"].string, it["isAdmin"].bool) }.toList()
	}

	fun getNonMembersOfContaining(name: String, id: Long, searchTerm: String, limit: Int): List<String> {
		return let(id be "id", name be "name", searchTerm be "searchTerm", limit be "limit").inQuery(
				"""
					MATCH (:User{name:{name}})-[:MEMBER_OF]->(g:Group) WHERE id(g) = {id}
					MATCH (u:User) WHERE toLower(u.name) CONTAINS toLower({searchTerm})
						AND NOT exists((u)-[:MEMBER_OF]->(g))
					RETURN u.name AS name LIMIT {limit}
				"""
		).map { it["name"].string }.toList()
	}

	fun getGroupsOf(name: String): List<Group> {
		return let(name be "name").inQuery(
				"""
					MATCH (u:User{name:{name}})-[m:MEMBER_OF]->(g:Group)
					OPTIONAL MATCH (g)<-[:CREATED]-(cu:User)
					RETURN g, id(g) AS id, cu.name AS cu, g.messageCount - m.seen AS unseen,
						exists((u)-[:ADMIN_OF]->(g)) AS isAdmin
				"""
		).map {
			it["g"].convertTo<Group>().apply {
				id = it["id"].long
				creatorName = it["cu"].stringOrNull
				unseenCount = it["unseen"].int
				isAdministrated = it["isAdmin"].bool
			}
		}.toList()
	}

	fun getInvitedUsersTo(user: String, groupId: Long): List<String> {
		return let(groupId be "id", user be "name").inQuery(
				"""
					MATCH (:User{name:{name}})-[:ADMIN_OF]->(g: Group) WHERE id(g) = {id}
					MATCH (u:User)-[:RECEIVED]->(:Invitation)<-[:HAS_INVITATION]-(g)
					RETURN u.name AS name
				"""
		).map { it["name"].string }.toList()
	}

	fun isPublicNameUsed(groupName: String): Boolean {
		return let(groupName be "name").inQuery(
				"""
					MATCH (g:Group{visibility:"PUBLIC", name:{name}}) RETURN count(g) > 0 AS used
				"""
		)["used"].bool
	}

	fun getPublicGroupsByNameContaining(searchTerm: String, limit: Int): List<PublicGroup> {
		return let(searchTerm be "searchTerm", limit be "limit").inQuery(
				"""
					MATCH (g:Group{visibility:"PUBLIC"}) WHERE toLower(g.name) CONTAINS toLower({searchTerm})
					RETURN g.name AS name, id(g) AS id LIMIT {limit}
				"""
		).map { PublicGroup(it["id"].long, it["name"].string) }.toList()
	}

	fun explorePublicGroupsByNameContaining(username: String, searchTerm: String, limit: Int): List<PublicGroup> {
		return let(username be "name", searchTerm be "searchTerm", limit be "limit").inQuery(
				"""
					MATCH (u:User{name:{name}})
					MATCH (g:Group{visibility:"PUBLIC"}) WHERE toLower(g.name) CONTAINS toLower({searchTerm})
						AND NOT exists((u)-[:MEMBER_OF]->(g))
					RETURN g.name AS name, id(g) AS id LIMIT {limit}
				"""
		).map { PublicGroup(it["id"].long, it["name"].string) }.toList()
	}

	fun enterPublicGroup(username: String, groupId: Long) {
		let(username be "name", groupId be "id").inQuery(
				"""
					MATCH (u:User{name:{name}})
					MATCH (g:Group{visibility:"PUBLIC"}) WHERE id(g) = {id}
					MERGE (u)-[:MEMBER_OF{seen:0}]->(g)
				"""
		)
	}

	fun deleteMember(adminName: String, memberName: String, groupId: Long) {
		let(adminName be "adminName", memberName be "memberName", groupId be "id").inQuery(
				"""
					MATCH (:User{name:{adminName}})-[:ADMIN_OF]->(g:Group) WHERE id(g) = {id}
					MATCH (m:User{name:{memberName}})-[r:MEMBER_OF]->(g)
						WHERE NOT exists((m)-[:ADMIN_OF]->(g))
					DELETE r
				"""
		)
	}

}