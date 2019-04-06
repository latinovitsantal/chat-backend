package hu.latantal.chat.data

import org.springframework.security.core.*
import org.springframework.security.core.userdetails.*

class User(val name: String, var password: String)

class UserDetailsImpl(private val user: User) : UserDetails {
	override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableSetOf()
	override fun isEnabled() = true
	override fun getUsername() = user.name
	override fun isCredentialsNonExpired() = true
	override fun getPassword() = user.password
	override fun isAccountNonExpired() = true
	override fun isAccountNonLocked() = true
}
val User.details get() = UserDetailsImpl(this)



