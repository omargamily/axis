package com.axis.pay.integration

import com.axis.pay.controller.dto.OpenAccountResponseDto
import com.axis.pay.controller.dto.SignInRequestDto
import com.axis.pay.controller.dto.SigninResponseDto
import com.axis.pay.controller.dto.TransactionRequestDto
import com.axis.pay.model.TransactionType
import com.axis.pay.model.User
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.TransactionRepository
import com.axis.pay.model.repository.UserRepository
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
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
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val testUserEmail = "accountcontroluser@example.com"
    private val testUserPassword = "password123"
    private lateinit var testUser: User
    private lateinit var jwtToken: String
    private lateinit var testAccountId: UUID

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port

        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        userRepository.deleteAll()


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

        val openAccountResponse = Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            post("/api/accounts/open")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(OpenAccountResponseDto::class.java)
        }
        testAccountId = openAccountResponse.accountId
    }

    @AfterEach
    fun cleanup() {
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `should Open Account Successfully For Authenticated User`() {
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
    fun `should Return Unauthorized When Opening Account Without Authentication`() {
        Given {
            contentType(ContentType.JSON)
        } When {
            post("/api/accounts/open")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }

    @Test
    fun `should Deposit Successfully`() {
        // 1. Open an account for the test user
        val openAccountResponse = Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            post("/api/accounts/open")
        } Then {
            statusCode(HttpStatus.OK.value())
        } Extract {
            body().`as`(OpenAccountResponseDto::class.java)
        }
        val accountId = openAccountResponse.accountId

        // 2. Deposit
        val depositAmount = 150.75
        val transactionRequest = TransactionRequestDto(amount = depositAmount)

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(transactionRequest)
        } When {
            post("/api/accounts/$accountId/deposit")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accountId", equalTo(accountId.toString()))
            body("type", equalTo(TransactionType.DEPOSIT.name))
            body("amount", equalTo(depositAmount.toFloat()))
            body("currentBalance", equalTo(depositAmount.toFloat()))
        }

        // 3. Verify balance via endpoint
        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            get("/api/accounts/$accountId/balance")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accountId", equalTo(accountId.toString()))
            body("balance", equalTo(depositAmount.toFloat()))
        }
    }


    @Test
    fun `should Return 400 For Invalid Deposit Amount`() {
        val transactionRequest = TransactionRequestDto(amount = -50.0)

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(transactionRequest)
        } When {
            post("/api/accounts/$testAccountId/deposit")
        } Then {
            statusCode(HttpStatus.BAD_REQUEST.value())
        }
    }


    @Test
    fun `should Withdraw Successfully`() {
        val initialDeposit = 200.0
        accountRepository.findById(testAccountId).ifPresent {
            it.balance = initialDeposit
            accountRepository.save(it)
        }

        val withdrawalAmount = 50.25
        val transactionRequest = TransactionRequestDto(amount = withdrawalAmount)

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(transactionRequest)
        } When {
            post("/api/accounts/$testAccountId/withdraw")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accountId", equalTo(testAccountId.toString()))
            body("type", equalTo(TransactionType.WITHDRAW.name))
            body("amount", equalTo(withdrawalAmount.toFloat()))
            body("currentBalance", equalTo((initialDeposit - withdrawalAmount).toFloat()))
        }

        val account = accountRepository.findById(testAccountId).get()
        assertThat(account.balance).isEqualTo(initialDeposit - withdrawalAmount)
    }

    @Test
    fun `should Return 409 For Insufficient Funds`() {
        accountRepository.findById(testAccountId).ifPresent {
            it.balance = 10.0
            accountRepository.save(it)
        }

        val withdrawalAmount = 50.0
        val transactionRequest = TransactionRequestDto(amount = withdrawalAmount)

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(transactionRequest)
        } When {
            post("/api/accounts/$testAccountId/withdraw")
        } Then {
            statusCode(HttpStatus.CONFLICT.value())
            body("message", equalTo("Insufficient funds for withdrawal. Current balance: 10.0"))
        }
    }

    @Test
    fun `should Get Balance Successfully`() {
        val expectedBalance = 777.77
        accountRepository.findById(testAccountId).ifPresent {
            it.balance = expectedBalance
            accountRepository.save(it)
        }

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            get("/api/accounts/$testAccountId/balance")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("accountId", equalTo(testAccountId.toString()))
            body("balance", equalTo(expectedBalance.toFloat()))
        }
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/balance - Should return 401 for unauthorized access")
    fun `should Return 401 For Unauthorized Balance Access`() {
        val otherUserEmail = "otherbaluser@example.com"
        userRepository.save(
            User(
                email = otherUserEmail,
                hashedPassword = passwordEncoder.encode("otherpass"),
                id = UUID.randomUUID()
            )
        )
        val otherJwtToken = Given {
            contentType(ContentType.JSON)
            body(SignInRequestDto(email = otherUserEmail, password = "otherpass"))
        } When { post("/auth/login") } Then { statusCode(HttpStatus.OK.value()) } Extract {
            body().`as`(
                SigninResponseDto::class.java
            ).accessToken
        }

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $otherJwtToken")
        } When {
            get("/api/accounts/$testAccountId/balance")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }

    @Test
    fun `should Return 404 For NonExistent Account Balance`() {
        val nonExistentAccountId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
        } When {
            get("/api/accounts/$nonExistentAccountId/balance")
        } Then {
            statusCode(HttpStatus.NOT_FOUND.value())
        }
    }

    @Test
    fun `multiple Deposits Should Reflect Correctly`() {
        val deposit1 = 50.0
        val deposit2 = 75.50
        val expectedBalance = deposit1 + deposit2

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(TransactionRequestDto(amount = deposit1))
        } When {
            post("/api/accounts/$testAccountId/deposit")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("currentBalance", equalTo(deposit1.toFloat()))
        }

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(TransactionRequestDto(amount = deposit2))
        } When {
            post("/api/accounts/$testAccountId/deposit")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("currentBalance", equalTo(expectedBalance.toFloat()))
        }

        val account = accountRepository.findById(testAccountId).get()
        assertThat(account.balance).isEqualTo(expectedBalance)

        Given {
            header("Authorization", "Bearer $jwtToken")
        } When {
            get("/api/accounts/$testAccountId/balance")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("balance", equalTo(expectedBalance.toFloat()))
        }
    }

    @Test
    fun `deposit Then Withdraw Should Be Transactional`() {
        val initialDeposit = 100.0
        val withdrawalAmount = 30.0
        val expectedFinalBalance = initialDeposit - withdrawalAmount

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(TransactionRequestDto(initialDeposit))
        } When {
            post("/api/accounts/$testAccountId/deposit")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("currentBalance", equalTo(initialDeposit.toFloat()))
        }

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(TransactionRequestDto(withdrawalAmount))
        } When {
            post("/api/accounts/$testAccountId/withdraw")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("currentBalance", equalTo(expectedFinalBalance.toFloat()))
        }

        val account = accountRepository.findById(testAccountId).get()
        assertThat(account.balance).isEqualTo(expectedFinalBalance)
        assertThat(transactionRepository.findAll().filter { it.account.id == testAccountId }.size).isEqualTo(2)
    }


    @Test
    fun `failed Withdraw Should Not Alter Balance`() {
        val initialDeposit = 50.0
        val acc = accountRepository.findById(testAccountId).get()
        acc.balance = initialDeposit
        accountRepository.save(acc)


        val withdrawalAmount = 100.0

        Given {
            contentType(ContentType.JSON)
            header("Authorization", "Bearer $jwtToken")
            body(TransactionRequestDto(withdrawalAmount))
        } When {
            post("/api/accounts/$testAccountId/withdraw")
        } Then {
            statusCode(HttpStatus.CONFLICT.value())
        }

        val accountAfterFailedWithdraw = accountRepository.findById(testAccountId).get()
        assertThat(accountAfterFailedWithdraw.balance).isEqualTo(initialDeposit)

        Given {
            header("Authorization", "Bearer $jwtToken")
        } When {
            get("/api/accounts/$testAccountId/balance")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("balance", equalTo(initialDeposit.toFloat()))
        }
        val transactions = transactionRepository.findAll().filter { it.account.id == testAccountId }
        assertThat(transactions.none { it.type == TransactionType.WITHDRAW }).isTrue()
    }
}