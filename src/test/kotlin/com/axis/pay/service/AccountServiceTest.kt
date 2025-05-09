package com.axis.pay.service

import com.axis.pay.model.Account
import com.axis.pay.model.User
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class AccountServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var accountRepository: AccountRepository

    @InjectMockKs
    private lateinit var accountService: AccountService

    private lateinit var testUserId: UUID
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUserId = UUID.randomUUID()
        testUser = User(id = testUserId, email = "test@example.com", hashedPassword = "password")
    }

    @Nested
    inner class OpenAccountTests {

        @Test
        fun `should open account successfully when user exists`() {
            val accountSlot = slot<Account>()
            val expectedAccountId = UUID.randomUUID()
            val savedAccount = Account(id = expectedAccountId, user = testUser)

            every { userRepository.findById(testUserId) } returns Optional.of(testUser)
            every { accountRepository.save(capture(accountSlot)) } returns savedAccount

            val resultAccountId = accountService.openAccount(testUserId)

            assertThat(resultAccountId).isEqualTo(expectedAccountId)
            verify(exactly = 1) { userRepository.findById(testUserId) }
            verify(exactly = 1) { accountRepository.save(any()) }

            val capturedAccount = accountSlot.captured
            assertThat(capturedAccount.user).isEqualTo(testUser)
            assertThat(capturedAccount.id).isNotNull()
        }

        @Test
        fun `should throw UsernameNotFoundException when user does not exist`() {
            every { userRepository.findById(testUserId) } returns Optional.empty()

            assertThatThrownBy { accountService.openAccount(testUserId) }
                .isInstanceOf(UsernameNotFoundException::class.java)
                .hasMessage("User not found with id: $testUserId")

            verify(exactly = 1) { userRepository.findById(testUserId) }
            verify(exactly = 0) { accountRepository.save(any()) }
        }
    }
}