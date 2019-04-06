package hu.latantal.chat.controller

import hu.latantal.chat.data.*
import hu.latantal.chat.service.*
import org.springframework.http.*
import org.springframework.http.HttpStatus.*
import org.springframework.web.bind.annotation.*
import java.security.*

@RestController
@RequestMapping("/contacts")
class ContactController(val contactService: ContactService) {

	@GetMapping
	fun get(principal: Principal) : ResponseEntity<List<Contact>> {
		val contacts = contactService.getContactsOf(principal.name)
		return ResponseEntity(contacts, OK)
	}

	@PutMapping("/requests")
	fun sendRequest(principal: Principal, @RequestParam receiver: String): ResponseEntity<Any> {
		contactService.sendRequest(principal.name, receiver)
		return ResponseEntity(OK)
	}

	@GetMapping("/requests")
	fun getRequests(principal: Principal): ResponseEntity<List<Request>> {
		val requests = contactService.getRequestsReceived(principal.name)
		return ResponseEntity(requests, OK)
	}

	@PostMapping("/requests/rejections")
	fun declineRequest(principal: Principal, @RequestParam sender: String): ResponseEntity<Any> {
		contactService.declineRequest(sender, principal.name)
		return ResponseEntity(OK)
	}

	@PostMapping("/requests/acceptances")
	fun acceptRequest(principal: Principal, @RequestParam sender: String): ResponseEntity<Any> {
		contactService.acceptRequest(sender, principal.name)
		return ResponseEntity(OK)
	}

}