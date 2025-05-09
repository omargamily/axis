package com.axis.pay.configuration

import com.axis.pay.model.User
import com.axis.pay.service.UserService
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JwtToUserPrincipalConverter(private val userService: UserService) : Converter<Jwt, AbstractAuthenticationToken> {

    private val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
        setAuthorityPrefix("ROLE_")
        setAuthoritiesClaimName("roles")
    }

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val userIdString = jwt.subject
            ?: throw UsernameNotFoundException("JWT subject (userId) cannot be null")

        val user: User = try {
            userService.findById(UUID.fromString(userIdString))
        } catch (e: UsernameNotFoundException) {
            throw UsernameNotFoundException("User with ID '$userIdString' not found, token invalid", e)
        } catch (e: IllegalArgumentException) {
            throw UsernameNotFoundException("Invalid UUID format in JWT subject: $userIdString", e)
        }

        val authorities = grantedAuthoritiesConverter.convert(jwt)
        return UsernamePasswordAuthenticationToken(user, jwt.tokenValue, authorities)
    }
}