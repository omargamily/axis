package com.axis.pay.service

import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class JwtService(private val jwtEncoder: JwtEncoder) {
    @Value("\${jwt.issuer}")
    private lateinit var jwtIssuer: String

    fun generateToken(id: String, roles: List<String>): String {
        val now = Instant.now()
        val expiry = 3600L // 1 hour

        val claims = JwtClaimsSet.builder()
            .issuer(jwtIssuer)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(id)
            .claim("roles", roles)
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()

        val jwtEncoderParameters = JwtEncoderParameters.from(jwsHeader, claims)
        return jwtEncoder.encode(jwtEncoderParameters).tokenValue
    }
}