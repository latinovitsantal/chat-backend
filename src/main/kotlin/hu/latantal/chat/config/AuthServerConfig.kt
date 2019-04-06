package hu.latantal.chat.config

import hu.latantal.chat.service.*
import org.springframework.context.annotation.*
import org.springframework.security.authentication.*
import org.springframework.security.crypto.password.*
import org.springframework.security.oauth2.config.annotation.configurers.*
import org.springframework.security.oauth2.config.annotation.web.configuration.*
import org.springframework.security.oauth2.config.annotation.web.configurers.*
import org.springframework.security.oauth2.provider.token.*


@Configuration
@EnableAuthorizationServer
class AuthServerConfig(
		private val tokenStore: TokenStore,
		private val authManager: AuthenticationManager,
		private val encoder: PasswordEncoder,
		private val userDetailsService: UserDetailsServiceImpl
) : AuthorizationServerConfigurerAdapter() {

	override fun configure(clients: ClientDetailsServiceConfigurer) {
		clients.inMemory()
				.withClient("latantal-chat")
				.secret(encoder.encode("super-secret"))
				.authorizedGrantTypes("password", "authorization_code", "refresh_token", "implicit")
				.scopes("read", "write", "trust")
				.accessTokenValiditySeconds(3600)
				.refreshTokenValiditySeconds(21600)
	}

	override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
		endpoints.tokenStore(tokenStore)
				.authenticationManager(authManager)
				.userDetailsService(userDetailsService)
	}

}