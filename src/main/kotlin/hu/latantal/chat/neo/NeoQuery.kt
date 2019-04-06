package hu.latantal.chat.neo

import org.neo4j.driver.internal.InternalRecord
import org.neo4j.driver.v1.Driver

class NeoQuery(private val driver: Driver, private val serializer: NeoSerializer) {

    fun submit(query: String, parameters: Map<String, Any?> = mapOf()): CursorWrapper {
        val session = driver.session()

        return session.use {
            val _parameters = serializer.serialize(parameters)
            val statementResult = session.run(query, _parameters)
            if (statementResult.hasNext()) {
                CursorWrapper(statementResult.peek(), statementResult)
            } else {
                CursorWrapper(InternalRecord(listOf(), arrayOf()), statementResult)
            }
        }
    }

}