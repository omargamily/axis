package com.axis.pay.controller

import com.axis.pay.controller.dto.SignInRequestDto
import com.axis.pay.controller.dto.SigninResponseDto
import com.axis.pay.controller.dto.SignupRequestDto
import com.axis.pay.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager
) {

    @PostMapping("/signup")
    fun signup(@Valid @RequestBody signupRequest: SignupRequestDto): String {
        val user = userService.createUser(signupRequest.email, signupRequest.password)
        return user.id.toString()
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody signinRequest: SignInRequestDto): SigninResponseDto {
        val authenticationToken = UsernamePasswordAuthenticationToken(signinRequest.email, signinRequest.password)
        val authentication = authenticationManager.authenticate(authenticationToken)
        SecurityContextHolder.getContext().authentication = authentication
        return userService.signIn(authentication)
    }
}