package com.axis.pay.service

import com.axis.pay.UnAuthorizedException
import com.axis.pay.controller.dto.SigninResponseDto
import com.axis.pay.model.User
import com.axis.pay.model.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository
    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var jwtService: JwtService

    private lateinit var userService: UserService

    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val encodedPassword = "encodedPassword123"
    private val testUserId = UUID.randomUUID()
    private val testUser = User(id = testUserId, email = testEmail, hashedPassword = encodedPassword)
    private val testJwtToken = "test.jwt.token"

    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, passwordEncoder, jwtService)
    }

    @Nested
    inner class LoadUserByUsernameTests {
        @Test
        fun `should throw UsernameNotFoundException when user is not found`() {
            every { userRepository.findByEmail(testEmail) } returns null

            assertThatThrownBy { userService.loadUserByUsername(testEmail) }
                .isInstanceOf(UsernameNotFoundException::class.java)
                .hasMessage("User not found with email: $testEmail")

            verify(exactly = 1) { userRepository.findByEmail(testEmail) }
        }

        @Test
        fun `should return UserDetails when user is found`() {
            every { userRepository.findByEmail(testEmail) } returns testUser

            val result = userService.loadUserByUsername(testEmail)

            assertThat(result).isEqualTo(testUser)
            verify(exactly = 1) { userRepository.findByEmail(testEmail) }
        }
    }

    @Nested
    inner class CreateUserTests {
        @Test
        fun `should encode password, save the new user, and return the saved user`() {
            val userSlot = slot<User>()
            val newUser = User(id = testUserId, email = testEmail, hashedPassword = encodedPassword)

            every { userRepository.findByEmail(testEmail) } returns null
            every { passwordEncoder.encode(testPassword) } returns encodedPassword
            every { userRepository.save(capture(userSlot)) } returns newUser

            val result = userService.createUser(testEmail, testPassword)

            assertThat(result.id).isEqualTo(testUserId)
            assertThat(result.email).isEqualTo(testEmail)
            assertThat(result.hashedPassword).isEqualTo(encodedPassword)

            verify(exactly = 1) { passwordEncoder.encode(testPassword) }
            verify(exactly = 1) { userRepository.save(any()) }

            val capturedUser = userSlot.captured
            assertThat(capturedUser.email).isEqualTo(testEmail)
            assertThat(capturedUser.hashedPassword).isEqualTo(encodedPassword)
            assertThat(capturedUser.id).isNotNull()
        }
    }

    @Nested
    inner class SignInTests {
        @Test
        fun `should return SigninResponseDto with JWT when authentication is valid and principal is User`() {
            val authentication = mockk<Authentication>()
            every { authentication.isAuthenticated } returns true
            every { authentication.principal } returns testUser
            every { jwtService.generateToken(testUserId.toString(), listOf("ROLE_USER")) } returns testJwtToken

            val result = userService.signIn(authentication)

            assertThat(result).isEqualTo(SigninResponseDto(testJwtToken))
            verify(exactly = 1) { jwtService.generateToken(testUserId.toString(), listOf("ROLE_USER")) }
        }

        @Test
        fun `should throw UnAuthorizedException when authentication principal is not User`() {
            val authentication = mockk<Authentication>()
            every { authentication.isAuthenticated } returns true
            every { authentication.principal } returns Any()

            assertThatThrownBy { userService.signIn(authentication) }
                .isInstanceOf(UnAuthorizedException::class.java)
                .hasMessage("User is not authenticated")
        }

        @Test
        fun `should throw UnAuthorizedException when authentication is not authenticated`() {
            val authentication = mockk<Authentication>()
            every { authentication.isAuthenticated } returns false

            assertThatThrownBy { userService.signIn(authentication) }
                .isInstanceOf(UnAuthorizedException::class.java)
                .hasMessage("User is not authenticated")
        }
    }
}
