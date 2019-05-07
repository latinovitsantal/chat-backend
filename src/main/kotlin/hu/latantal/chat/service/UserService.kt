package hu.latantal.chat.service

import hu.latantal.chat.data.*
import hu.latantal.chat.data.User
import hu.latantal.chat.neo.*
import org.springframework.security.core.userdetails.*
import org.springframework.security.crypto.password.*
import org.springframework.stereotype.*

@Service
class UserDetailsServiceImpl(val userService: UserService) : UserDetailsService {

	override fun loadUserByUsername(username: String): UserDetails {
		return userService.findByName(username)?.details
				?: throw UsernameNotFoundException("$username not found")
	}

}


@Service
class UserService(val passwordEncoder: PasswordEncoder) {

	fun findByName(name: String): User? {
		return let(name be "name").inQuery(
				"MATCH (user:User{name:{name}}) RETURN user"
		).single("user")
	}

	fun isNameUsed(name: String): Boolean {
		return let(name be "name").inQuery(
				"MATCH (u:User{name:{name}}) RETURN count(u) > 0 AS isUsed"
		)["isUsed"].bool
	}

	fun deleteByName(name: String): Boolean {
		return let(name be "n").inQuery(
				"MATCH (u:User{name:{n}}) DETACH DELETE u RETURN count(u) > 0 AS existed"
		)["existed"].bool
	}

	fun register(name: String, password: String): Boolean {
		val encodedPassword = passwordEncoder.encode(password)
		return let(name be "n", encodedPassword be "p").inQuery(
				"""
					MERGE (l:Lock{name:"register"}) SET l.lock = TRUE
					WITH l
					MATCH (u:User{name:{n}})
					WITH CASE WHEN count(u) > 0 THEN [] ELSE [1] END AS unused
					FOREACH (_ IN unused |
						CREATE (u:User{name:{n},password:{p}})
					)
					WITH unused
					MERGE (l:Lock{name:"register"}) REMOVE l.lock RETURN size(unused) = 1 AS success
				"""
		)["success"].bool
	}

	fun refreshPassword(name: String, password: String) {
		val encodedPassword = passwordEncoder.encode(password)
		let(name be "n", encodedPassword be "p").inQuery(
				"MATCH (u:User{name:{n}}) SET u.password = {p}"
		)
	}

	fun findStrangerByNameContains(name: String, searchTerm: String, limit: Int): List<String> {
		return let(name be "name", searchTerm be "searchTerm", limit be "limit").inQuery(
				"""
					MATCH (u:User{name:{name}})
					MATCH (s:User) WHERE toLower(s.name) CONTAINS toLower({searchTerm})
						AND NOT exists((s)-[:HAS_CONTACT]->(:Contact)<-[:HAS_CONTACT]-(u))
						AND NOT exists((s)-[:REQUESTED]->(u))
						AND NOT exists((u)-[:REQUESTED]->(s))
					RETURN s.name AS name LIMIT {limit}
				"""
		).map { it["name"].string }.toList()
	}

}