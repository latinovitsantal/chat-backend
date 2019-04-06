package hu.latantal.chat.controller

import hu.latantal.chat.data.*
import hu.latantal.chat.data.Visibility.*
import hu.latantal.chat.service.*
import org.apache.coyote.*
import org.springframework.http.*
import org.springframework.http.HttpStatus.*
import org.springframework.web.bind.annotation.*
import java.security.*

@RestController
@RequestMapping("/groups")
class GroupController(val groupService: GroupService) {

	@GetMapping
	fun get(principal: Principal): ResponseEntity<List<Group>> {
		val groups = groupService.getGroupsOf(principal.name)
		return ResponseEntity(groups, OK)
	}

	@GetMapping("/members")
	fun getMembers(
			principal: Principal,
			@RequestParam groupId: Long
	): ResponseEntity<List<Member>> {
		val members = groupService.getMembersOf(principal.name, groupId)
		return ResponseEntity(members, OK)
	}

	@GetMapping("/nonmembers")
	fun getNonMembers(
			principal: Principal,
			@RequestParam searchTerm: String,
			@RequestParam groupId: Long,
			@RequestParam limit: Int
	): ResponseEntity<List<String>> {
		val nonmembers = groupService.getNonMembersOfContaining(principal.name, groupId, searchTerm, limit)
		return ResponseEntity(nonmembers, OK)
	}

	@GetMapping("/invitations")
	fun getInvitations(principal: Principal): ResponseEntity<List<Invitation>> {
		val invs = groupService.getInvitations(principal.name)
		return ResponseEntity(invs, OK)
	}

	@PutMapping("/invitations")
	fun sendInvitation(
			principal: Principal,
			@RequestParam groupId: Long,
			@RequestParam receiver: String
	): ResponseEntity<Any> {
		val isSent = groupService.sendInvitation(principal.name, receiver, groupId)
		return if (isSent) ResponseEntity(OK) else ResponseEntity(BAD_REQUEST)
	}

	@PostMapping("/invitations/rejections")
	fun rejectInvitation(principal: Principal, groupId: Long): ResponseEntity<Any> {
		val isRejected = groupService.declineInvitation(principal.name, groupId)
		return if (isRejected) ResponseEntity(OK) else ResponseEntity(BAD_REQUEST)
	}

	@PostMapping("/invitations/acceptances")
	fun acceptInvitation(principal: Principal, groupId: Long): ResponseEntity<Any> {
		val isAccepted = groupService.acceptInvitation(principal.name, groupId)
		return if (isAccepted) ResponseEntity(OK) else ResponseEntity(BAD_REQUEST)
	}

	@GetMapping("/invitations/invited")
	fun getInvitedUsers(principal: Principal, @RequestParam groupId: Long): ResponseEntity<List<String>> {
		val users = groupService.getInvitedUsersTo(principal.name, groupId)
		return ResponseEntity(users, OK)
	}

	@PostMapping
	fun createGroup(
			principal: Principal,
			@RequestParam groupName: String,
			@RequestParam type: Visibility
	): ResponseEntity<Any> {
		val name = groupName.trim()
		if (name.isEmpty()) return ResponseEntity(BAD_REQUEST)
		return if (type == PRIVATE) {
			groupService.createPrivateGroup(principal.name, name)
			ResponseEntity(CREATED)
		} else {
			val created = groupService.createPublicGroup(principal.name, name)
			if (created) ResponseEntity(CREATED) else ResponseEntity(BAD_REQUEST)
		}
	}

	@GetMapping("/nameUsage")
	fun isPublicNameUsed(@RequestParam name: String): ResponseEntity<Boolean> {
		val isUsed = groupService.isPublicNameUsed(name)
		return ResponseEntity(isUsed, OK)
	}

	@GetMapping("/explore")
	fun explorePublicGroups(
			principal: Principal,
			@RequestParam searchTerm: String,
			limit: Int
	): ResponseEntity<List<PublicGroup>> {
		val groups = groupService.explorePublicGroupsByNameContaining(principal.name, searchTerm, limit)
		return ResponseEntity(groups, OK)
	}

	@PutMapping("/entries")
	fun enterPublicGroup(principal: Principal, @RequestParam groupId: Long): ResponseEntity<Any> {
		groupService.enterPublicGroup(principal.name, groupId)
		return ResponseEntity(OK)
	}

	@DeleteMapping("/members")
	fun deleteMember(
			principal: Principal,
			@RequestParam username: String,
			@RequestParam groupId: Long
	): ResponseEntity<Any> {
		groupService.deleteMember(principal.name, username, groupId)
		return ResponseEntity(OK)
	}

}