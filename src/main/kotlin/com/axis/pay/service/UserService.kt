package com.axis.pay.service

import com.axis.pay.ResourceConflictException
import com.axis.pay.UnAuthorizedException
import com.axis.pay.controller.dto.SigninResponseDto
import com.axis.pay.controller.dto.SignupRequestDto
import com.axis.pay.model.User
import com.axis.pay.model.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): UserDetails {
        return userRepository.findByEmail(email) ?: throw UsernameNotFoundException("User not found with email: $email")
    }

    fun findById(id: UUID): User {
        return userRepository.findByIdOrNull(id) ?: throw UsernameNotFoundException("User not found with id: $id")
    }

    fun createUser(email: String, password: String): User {
        if (userRepository.findByEmail(email) != null) {
            throw ResourceConflictException("User already exists")
        }
        val newUser = User(id = UUID.randomUUID(), email = email, hashedPassword = passwordEncoder.encode(password))
        return userRepository.save(newUser)
    }

    fun signIn(authentication: Authentication): SigninResponseDto {
        return if (authentication.isAuthenticated && authentication.principal is User) {
            val user = authentication.principal as User
            val id = user.id.toString()
            val roles = user.authorities.map { it.authority }
            SigninResponseDto(jwtService.generateToken(id, roles))
        } else {
            throw UnAuthorizedException("User is not authenticated")
        }
    }
}