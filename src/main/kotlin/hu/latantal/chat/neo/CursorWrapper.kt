package hu.latantal.chat.neo

import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.StatementResult

class CursorWrapper(private val record: Record, private val sr: StatementResult) : Sequence<CursorWrapper> {

    private class CursorIterator(val sr: StatementResult) : Iterator<CursorWrapper> {
        override fun next() = CursorWrapper(sr.next(), sr)
        override fun hasNext() = sr.hasNext()
    }

    override fun iterator(): Iterator<CursorWrapper> = CursorIterator(sr)

    fun unwrap(key: String) = Cursor(record[key])

}
