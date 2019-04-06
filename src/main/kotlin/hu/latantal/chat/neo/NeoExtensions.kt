package hu.latantal.chat.neo

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import org.intellij.lang.annotations.*
import org.neo4j.driver.v1.*

object Neo {

	val driver = GraphDatabase.driver(
			"bolt://localhost:7687",
			AuthTokens.basic("neo4j", "demo")
	)

	val neo = NeoQuery(driver, DefaultNeoSerializer())

	fun deleteAllNodes() {
		invoke("MATCH (n) DETACH DELETE n")
	}

	operator fun invoke(query: String, params: Map<String, Any>) = neo.submit(query, params)
	operator fun invoke(query: String, vararg param: Pair<String, Any>) = neo.submit(query, mapOf(*param))

}

val mapper = ObjectMapper().registerModule(KotlinModule())!!
inline fun <reified T> Cursor.convertTo() = mapper.convertValue<T>(this.asMap())
fun String.jsonParse() = mapper.readValue(this, Map::class.java)

operator fun CursorWrapper.get(key: String) = unwrap(key)
inline fun <reified T> CursorWrapper.all(key: String) = map { it[key].convertTo<T>() }.toList()
inline fun <reified T> CursorWrapper.first(key: String) = all<T>(key).firstOrNull()
inline fun <reified T> CursorWrapper.single(key: String) = all<T>(key).singleOrNull()

fun let(vararg param: Pair<String, Any>) = mapOf(*param)
infix fun Any.be(param: String) = Pair(param, this)
fun Map<String,Any>.inQuery(@Language("Cypher") query: String) = Neo(query, this)
