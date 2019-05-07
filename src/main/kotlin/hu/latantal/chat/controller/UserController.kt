package hu.latantal.chat.controller

import hu.latantal.chat.data.*
import hu.latantal.chat.service.*
import org.springframework.http.*
import org.springframework.http.HttpStatus.*
import org.springframework.web.bind.annotation.*
import java.security.*

@RestController
@RequestMapping("/users")
class UserController(val userService: UserService) {

	@GetMapping("/me")
	fun me(principal: Principal) = ResponseEntity(principal.name!!, OK)

	@GetMapping
	fun searchByName(
			principal: Principal,
			@RequestParam searchTerm: String,
			@RequestParam limit: Int
	): ResponseEntity<List<String>> {
		val users = userService.findStrangerByNameContains(principal.name, searchTerm, limit)
		return ResponseEntity(users, OK)
	}

}