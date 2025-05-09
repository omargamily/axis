package com.axis.pay.integration

import com.axis.pay.controller.dto.SignInRequestDto
import com.axis.pay.controller.dto.SigninResponseDto
import com.axis.pay.model.User
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val testUserEmail = "accountuser@example.com"
    private val testUserPassword = "password123"
    private lateinit var testUser: User
    private lateinit var jwtToken: String

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
        testUser = userRepository.save(
            User(
                email = testUserEmail,
                hashedPassword = passwordEncoder.encode(testUserPassword),
                id = UUID.randomUUID()
            )
        )

        val signinResponse = Given {
            contentType(ContentType.JSON)
            body(SignInRequestDto(email = testUserEmail, password = testUserPassword))
        } When {
            post("/auth/login")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(SigninResponseDto::class.java)
        }
        jwtToken = signinResponse.accessToken
    }

    @AfterEach
    fun cleanup() {
        accountRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("POST /api/accounts/open - Should open account successfully for authenticated user")
    fun `shouldOpenAccountSuccessfullyForAuthenticatedUser`() {
        val response = Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            post("/api/accounts/open")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accountId", notNullValue())
        } Extract {
            body().jsonPath().getString("accountId")
        }

        val accountId = UUID.fromString(response)
        assertThat(accountId).isNotNull()

        val account = accountRepository.findById(accountId)
        assertThat(account).isPresent
        account.ifPresent {
            assertThat(it.user.id).isEqualTo(testUser.id)
        }
    }

    @Test
    @DisplayName("POST /api/accounts/open - Should return 401 Unauthorized when opening account without authentication")
    fun `shouldReturnUnauthorizedWhenOpeningAccountWithoutAuthentication`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            post("/api/accounts/open")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }
}