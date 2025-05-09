package com.axis.pay.service

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class Configurations {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecretString: String


    @Bean
    fun jwtSecretKey(): SecretKey {
        val secretBytes = jwtSecretString.toByteArray(StandardCharsets.UTF_8)
        return SecretKeySpec(secretBytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(secretKey: SecretKey): JwtEncoder {
        val jwkSource: JWKSource<SecurityContext> = ImmutableSecret(secretKey)
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    fun jwtDecoder(secretKey: SecretKey): JwtDecoder {
        return NimbusJwtDecoder.withSecretKey(secretKey).build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}