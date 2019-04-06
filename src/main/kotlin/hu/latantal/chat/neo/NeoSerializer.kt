package hu.latantal.chat.neo


interface NeoSerializer {
    fun serialize(parameters: Map<String, Any?>): Map<String, Any?>
}