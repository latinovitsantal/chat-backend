package hu.latantal.chat.data

data class Message(var id: Long, var text: String, var time: String, var username: String?) {
	val isOwned = username != null
}

data class Request(var username: String, var time: String)

data class Contact(var id: Long, var username: String?, var unseenCount: Int, var lastUpdated: String)

enum class Visibility { PUBLIC, PRIVATE }

data class Group(
		var id: Long,
		var name: String,
		var creatorName: String?,
		var messageCount: Int,
		var unseenCount: Int,
		var createdAt: String,
		var visibility: Visibility,
		var lastUpdated: String,
		var isAdministrated: Boolean
)

data class PublicGroup(var id: Long, var name: String)

data class Invitation(var groupId: Long, var groupName: String, var sender: String?, var time: String)

data class Member(var name: String, var isAdmin: Boolean)