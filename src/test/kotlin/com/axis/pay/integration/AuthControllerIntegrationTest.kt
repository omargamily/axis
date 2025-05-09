package com.axis.pay.integration

import com.axis.pay.controller.dto.SignInRequestDto
import com.axis.pay.controller.dto.SignupRequestDto
import com.axis.pay.model.User
import com.axis.pay.model.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.servlet.function.RequestPredicates.contentType
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        userRepository.deleteAll()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
    }

    @Test
    fun `should register a new user successfully (Happy Path)`() {
        val signupRequest = SignupRequestDto(
            email = "testuser@example.com",
            password = "password123"
        )

        val responseBody = Given {
            contentType(ContentType.JSON)
            body(signupRequest)
        } When {
            post("/auth/signup")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().asString()
        }

        assertThat(responseBody).isNotNull()
        assertThat(responseBody).isNotEmpty()

        val user = userRepository.findByEmail(signupRequest.email)
        assertThat(user).isNotNull
        user?.let {
            assertThat(it.email).isEqualTo(signupRequest.email)
            assertThat(it.id.toString()).isEqualTo(responseBody)
        }
    }

    @Test
    fun `should return 409 when email already exists`() {
        val existingUserRequest = SignupRequestDto(
            email = "existing@example.com",
            password = "password123"
        )

        userRepository.save(
            User(
                email = existingUserRequest.email,
                hashedPassword = passwordEncoder.encode(existingUserRequest.password),
                id = UUID.randomUUID()
            )
        )

        Given {
            contentType(ContentType.JSON)
            body(existingUserRequest)
        } When {
            post("/auth/signup")
        } Then {
            statusCode(HttpStatus.CONFLICT.value())
            body("message", equalTo("User already exists"))
        }
    }

    @Test
    fun `should return 400 for invalid signup request (e g , blank email)`() {
        val signupRequest = SignupRequestDto(
            email = "",
            password = "password123"
        )

        Given {
            contentType(ContentType.JSON)
            body(signupRequest)
        } When {
            post("/auth/signup")
        } Then {
            statusCode(HttpStatus.BAD_REQUEST.value())
        }
    }

    @Test
    fun `should login successfully with valid credentials`() {
        val userEmail = "loginuser@example.com"
        val rawPassword = "password123"
        val encodedPassword = passwordEncoder.encode(rawPassword)
        userRepository.save(User(email = userEmail, hashedPassword = encodedPassword, id = UUID.randomUUID()))

        Given {
            contentType(ContentType.JSON)
            body(SignInRequestDto(email = userEmail, password = rawPassword))
        } When {
            post("/auth/login")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accessToken", notNullValue())
            body("accessToken", not(emptyString()))
        }
    }

    @Test
    fun `should return 401 on login with invalid password`() {
        val userEmail = "userpass@example.com"
        val rawPassword = "password123"
        val encodedPassword = passwordEncoder.encode(rawPassword)
        userRepository.save(User(email = userEmail, hashedPassword = encodedPassword, id = UUID.randomUUID()))

        Given {
            contentType(ContentType.JSON)
            body(SignInRequestDto(email = userEmail, password = "wrongPassword"))
        } When {
            post("/auth/login")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }

    @Test
    fun `should return 401 on login with non-existent user`() {
        Given {
            contentType(ContentType.JSON)
            body(SignInRequestDto(email = "nonexistent@example.com", password = "anypassword"))
        } When {
            post("/auth/login")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }
}