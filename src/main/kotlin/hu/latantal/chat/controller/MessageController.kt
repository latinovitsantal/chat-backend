package hu.latantal.chat.controller

import hu.latantal.chat.data.*
import hu.latantal.chat.service.*
import org.springframework.http.*
import org.springframework.http.HttpStatus.*
import org.springframework.web.bind.annotation.*
import java.security.*

@RestController
@RequestMapping("/messages")
class MessageController(val messageService: MessageService) {

	@GetMapping
	fun get(
			principal: Principal,
			@RequestParam id: Long,
			@RequestParam limit: Int,
			@RequestParam(required = false) before: Long?
	) : ResponseEntity<List<Message>> {
		val messages = if (before == null) messageService.getMessages(principal.name, id, limit)
		else messageService.getPrevMessages(principal.name, id, before, limit)
		return ResponseEntity(messages, OK)
	}

	data class PostMessage(var id: Long, var text: String)
	@PostMapping
	fun send(principal: Principal, @RequestBody postMessage: PostMessage): ResponseEntity<Message?> {
		var (id, text) = postMessage
		text = text.trim()
		if (text.isEmpty()) return ResponseEntity(OK)
		val message = messageService.send(principal.name, id, text)
		return ResponseEntity(message, OK)
	}

	@PostMapping("/see")
	fun see(principal: Principal,
					@RequestParam convoId: Long,
					@RequestParam lastSeenId: Long
	): ResponseEntity<List<Message>> {
		val newMessages = messageService.see(principal.name, convoId, lastSeenId)
		return ResponseEntity(newMessages, OK)
	}

}