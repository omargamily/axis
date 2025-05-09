package com.axis.pay.configuration

import com.axis.pay.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder
) {

    @Value("\${jwt.issuer}")
    private lateinit var jwtIssuer: String

    @Bean
    fun jwtIssuer(): String = jwtIssuer

    @Bean
    fun authenticationManager(authProvider: DaoAuthenticationProvider): AuthenticationManager =
        ProviderManager(authProvider)

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userService)
        authProvider.setPasswordEncoder(passwordEncoder)
        return authProvider
    }

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtToUserPrincipalAuthenticationConverter: Converter<Jwt, AbstractAuthenticationToken>
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/auth/signup", "/error").permitAll()
                    .requestMatchers("/auth/login").permitAll()
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                    .requestMatchers("/webjars/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwtConfigurer ->
                    jwtConfigurer.jwtAuthenticationConverter(jwtToUserPrincipalAuthenticationConverter)
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        return http.build()
    }
}