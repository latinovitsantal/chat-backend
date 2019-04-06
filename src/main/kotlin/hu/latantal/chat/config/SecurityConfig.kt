package hu.latantal.chat.config

import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.method.configuration.*
import org.springframework.security.config.annotation.web.builders.*
import org.springframework.security.config.annotation.web.configuration.*
import org.springframework.security.crypto.bcrypt.*
import org.springframework.security.oauth2.provider.token.store.*
import org.springframework.web.cors.*

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig : WebSecurityConfigurerAdapter() {

	@Bean
	override fun authenticationManagerBean() = super.authenticationManagerBean()!!

	@Bean
	fun encoder() = BCryptPasswordEncoder()

	@Bean
	fun tokenStore() = InMemoryTokenStore()

	@Bean
	fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
		val config = CorsConfiguration().apply {
			allowedOrigins = listOf("*")
			allowedMethods = listOf("*")
			allowedHeaders = listOf("*")
			allowCredentials = true
		}
		return UrlBasedCorsConfigurationSource().apply {
			registerCorsConfiguration("/**", config)
		}
	}

	override fun configure(http: HttpSecurity) {
		http.cors().and()
				.csrf().disable()
				.anonymous().disable()
				.authorizeRequests()
				.antMatchers("/api-docs/**").permitAll()
	}

}