package hu.latantal.chat.controller

import hu.latantal.chat.data.*
import hu.latantal.chat.neo.*
import hu.latantal.chat.service.*
import org.springframework.http.*
import org.springframework.http.HttpStatus.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/public")
class PublicController(
		val userService: UserService,
		val groupService: GroupService,
		val messageService: MessageService
) {

	@PostMapping("/register")
	fun register(@RequestBody user: User) : ResponseEntity<String> {
		val created = userService.register(user.name, user.password)
		return if (created) ResponseEntity(HttpStatus.CREATED)
		else ResponseEntity("Username already in use", HttpStatus.BAD_REQUEST)
	}

	@GetMapping("/nameUsage")
	fun isPasswordUsed(@RequestParam name: String): ResponseEntity<Boolean> {
		val isUsed = userService.isNameUsed(name)
		return ResponseEntity(isUsed, OK)
	}

	@GetMapping("/groups")
	fun getGroupsContaining(
			@RequestParam(value = "") searchTerm: String,
			@RequestParam limit: Int
	): ResponseEntity<List<PublicGroup>> {
		val groups = groupService.getPublicGroupsByNameContaining(searchTerm, limit)
		return ResponseEntity(groups, OK)
	}

	@GetMapping("/messages")
	fun getMessagesOfPublicGroup(
			@RequestParam groupId: Long,
			@RequestParam(required = false) before: Long?,
			@RequestParam limit: Int
	): ResponseEntity<List<Message>> {
		val messages = if (before != null) messageService.getPrevMessagesOfPublicGroup(groupId, before, limit)
		else messageService.getMessagesOfPublicGroup(groupId, limit)
		return ResponseEntity(messages, OK)
	}

	@GetMapping("/messages/new")
	fun getNewMessagesOfPublicGroup(
			@RequestParam groupId: Long,
			@RequestParam lastSeenId: Long
	) : ResponseEntity<List<Message>> {
		val newMessages = messageService.getNewMessagesOfPublicGroup(groupId, lastSeenId)
		return ResponseEntity(newMessages, OK)
	}

}