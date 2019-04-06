package hu.latantal.chat.config

import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.web.builders.*
import org.springframework.security.oauth2.config.annotation.web.configuration.*
import org.springframework.security.oauth2.config.annotation.web.configurers.*
import org.springframework.security.oauth2.provider.error.*

@Configuration
@EnableResourceServer
class ResourceServerConfig : ResourceServerConfigurerAdapter() {

	override fun configure(resources: ResourceServerSecurityConfigurer) {
		resources.resourceId("resource_id").stateless(false)
	}

	override fun configure(http: HttpSecurity) {
		http.authorizeRequests()
				.antMatchers("/public/**").permitAll()
				.anyRequest().authenticated()
				.and().exceptionHandling().accessDeniedHandler(OAuth2AccessDeniedHandler())
	}

}