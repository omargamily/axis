package com.axis.pay.service

import com.axis.pay.BadRequestException
import com.axis.pay.ResourceConflictException
import com.axis.pay.ResourceNotFoundException
import com.axis.pay.UnAuthorizedException
import com.axis.pay.model.Account
import com.axis.pay.model.Transaction
import com.axis.pay.model.TransactionType
import com.axis.pay.model.User
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.TransactionRepository
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

    @MockK
    private lateinit var transactionRepository: TransactionRepository

    @InjectMockKs
    private lateinit var accountService: AccountService

    private lateinit var testUserId: UUID
    private lateinit var otherUserId: UUID
    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var testAccountId: UUID

    @BeforeEach
    fun setUp() {
        testUserId = UUID.randomUUID()
        otherUserId = UUID.randomUUID()
        testUser = User(id = testUserId, email = "test@example.com", hashedPassword = "password")
        otherUser = User(id = otherUserId, email = "other@example.com", hashedPassword = "password")
        testAccountId = UUID.randomUUID()
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

    @Nested
    inner class DepositTests {
        @Test
        fun `should deposit successfully and create transaction`() {
            val accountId = UUID.randomUUID()
            val depositAmount = 100.0
            val initialBalance = 50.0
            val user = User(id = testUserId, email = "test@example.com", hashedPassword = "pwd")
            val account = Account(id = accountId, user = user, balance = initialBalance)
            val transactionSlot = slot<Transaction>()

            every { accountRepository.findById(accountId) } returns Optional.of(account)
            every { accountRepository.save(any<Account>()) } answers { firstArg() } // Return the same account that was passed to save
            every { transactionRepository.save(capture(transactionSlot)) } answers { firstArg() } // Return the same transaction

            val resultTransaction = accountService.deposit(accountId, depositAmount, testUserId)

            assertThat(resultTransaction.type).isEqualTo(TransactionType.DEPOSIT)
            assertThat(resultTransaction.amount).isEqualTo(depositAmount)
            assertThat(resultTransaction.account.id).isEqualTo(accountId)
            assertThat(account.balance).isEqualTo(initialBalance + depositAmount)

            verify(exactly = 1) { accountRepository.findById(accountId) }
            verify(exactly = 1) { accountRepository.save(account) }
            verify(exactly = 1) { transactionRepository.save(any()) }

            val capturedTransaction = transactionSlot.captured
            assertThat(capturedTransaction.type).isEqualTo(TransactionType.DEPOSIT)
            assertThat(capturedTransaction.amount).isEqualTo(depositAmount)
        }

        @Test
        fun `deposit should throw IllegalArgumentException for non-positive amount`() {
            val accountId = UUID.randomUUID()
            assertThatThrownBy { accountService.deposit(accountId, 0.0, testUserId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Deposit amount must be positive")
            assertThatThrownBy { accountService.deposit(accountId, -10.0, testUserId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Deposit amount must be positive")
        }

        @Test
        fun `deposit should throw EntityNotFoundException when account not found`() {
            val accountId = UUID.randomUUID()
            every { accountRepository.findById(accountId) } returns Optional.empty()

            assertThatThrownBy { accountService.deposit(accountId, 100.0, testUserId) }
                .isInstanceOf(ResourceNotFoundException::class.java)
                .hasMessage("Account not found with id: $accountId")
        }

        @Test
        fun `deposit should throw UnAuthorizedException if user is not authorized`() {
            val accountId = UUID.randomUUID()
            val otherUserId = UUID.randomUUID()
            val user = User(id = otherUserId, email = "other@example.com", hashedPassword = "pwd")
            val account = Account(id = accountId, user = user, balance = 50.0)

            every { accountRepository.findById(accountId) } returns Optional.of(account)

            assertThatThrownBy { accountService.deposit(accountId, 100.0, testUserId) }
                .isInstanceOf(UnAuthorizedException::class.java)
                .hasMessage("User not authorized to deposit to this account")
        }
    }

    @Nested
    inner class WithdrawTests {
        @Test
        fun `should withdraw successfully and create transaction`() {
            val withdrawalAmount = 50.0
            val initialBalance = 100.0
            val account = Account(id = testAccountId, user = testUser, balance = initialBalance)
            val transactionSlot = slot<Transaction>()

            every { accountRepository.findById(testAccountId) } returns Optional.of(account)
            every { accountRepository.save(any<Account>()) } answers { firstArg() }
            every { transactionRepository.save(capture(transactionSlot)) } answers { firstArg() }

            val resultTransaction = accountService.withdraw(testAccountId, withdrawalAmount, testUserId)

            assertThat(resultTransaction.type).isEqualTo(TransactionType.WITHDRAW)
            assertThat(resultTransaction.amount).isEqualTo(withdrawalAmount)
            assertThat(resultTransaction.account.id).isEqualTo(testAccountId)
            assertThat(account.balance).isEqualTo(initialBalance - withdrawalAmount)

            verify(exactly = 1) { accountRepository.findById(testAccountId) }
            verify(exactly = 1) { accountRepository.save(account) }
            verify(exactly = 1) { transactionRepository.save(any()) }

            val capturedTransaction = transactionSlot.captured
            assertThat(capturedTransaction.type).isEqualTo(TransactionType.WITHDRAW)
            assertThat(capturedTransaction.amount).isEqualTo(withdrawalAmount)
        }

        @Test
        fun `withdraw should throw IllegalArgumentException for non-positive amount`() {
            assertThatThrownBy { accountService.withdraw(testAccountId, 0.0, testUserId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Withdrawal amount must be positive")
            assertThatThrownBy { accountService.withdraw(testAccountId, -10.0, testUserId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Withdrawal amount must be positive")
        }

        @Test
        fun `withdraw should throw EntityNotFoundException when account not found`() {
            every { accountRepository.findById(testAccountId) } returns Optional.empty()

            assertThatThrownBy { accountService.withdraw(testAccountId, 50.0, testUserId) }
                .isInstanceOf(ResourceNotFoundException::class.java)
                .hasMessage("Account not found with id: $testAccountId")
        }

        @Test
        fun `withdraw should throw UnAuthorizedException if user is not authorized for account`() {
            val account = Account(id = testAccountId, user = otherUser, balance = 100.0)
            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            assertThatThrownBy { accountService.withdraw(testAccountId, 50.0, testUserId) }
                .isInstanceOf(UnAuthorizedException::class.java)
                .hasMessage("User not authorized to withdraw from this account")
        }

        @Test
        fun `withdraw should throw ResourceConflictException for insufficient funds`() {
            val withdrawalAmount = 100.0
            val initialBalance = 50.0
            val account = Account(id = testAccountId, user = testUser, balance = initialBalance)

            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            assertThatThrownBy { accountService.withdraw(testAccountId, withdrawalAmount, testUserId) }
                .isInstanceOf(BadRequestException::class.java)
                .hasMessage("Insufficient funds for withdrawal. Current balance: $initialBalance")
        }
    }

    @Nested
    inner class GetBalanceTests {
        @Test
        fun `should return correct balance for authorized user`() {
            val expectedBalance = 123.45
            val account = Account(id = testAccountId, user = testUser, balance = expectedBalance)
            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            val balance = accountService.getBalance(testAccountId, testUserId)

            assertThat(balance).isEqualTo(expectedBalance)
            verify(exactly = 1) { accountRepository.findById(testAccountId) }
        }

        @Test
        fun `getBalance should throw EntityNotFoundException when account not found`() {
            every { accountRepository.findById(testAccountId) } returns Optional.empty()

            assertThatThrownBy { accountService.getBalance(testAccountId, testUserId) }
                .isInstanceOf(ResourceNotFoundException::class.java)
                .hasMessage("Account not found with id: $testAccountId")
        }

        @Test
        fun `getBalance should throw UnAuthorizedException if user is not authorized for account`() {
            val account = Account(id = testAccountId, user = otherUser, balance = 100.0)
            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            assertThatThrownBy { accountService.getBalance(testAccountId, testUserId) }
                .isInstanceOf(UnAuthorizedException::class.java)
                .hasMessage("User not authorized to view this account's balance")
        }
    }

    @Nested
    inner class FindAccountByIdAndUserIdTests {
        @Test
        fun `should return account if found and user matches`() {
            val account = Account(id = testAccountId, user = testUser, balance = 100.0)
            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            val result = accountService.findAccountByIdAndUserId(testAccountId, testUserId)

            assertThat(result).isEqualTo(account)
            verify(exactly = 1) { accountRepository.findById(testAccountId) }
        }

        @Test
        fun `should throw EntityNotFoundException if account not found`() {
            every { accountRepository.findById(testAccountId) } returns Optional.empty()

            assertThatThrownBy { accountService.findAccountByIdAndUserId(testAccountId, testUserId) }
                .isInstanceOf(ResourceNotFoundException::class.java)
                .hasMessage("Account not found or access denied for account id: $testAccountId")
        }

        @Test
        fun `should throw EntityNotFoundException if account found but user does not match`() {
            val account = Account(id = testAccountId, user = otherUser, balance = 100.0)
            every { accountRepository.findById(testAccountId) } returns Optional.of(account)

            assertThatThrownBy { accountService.findAccountByIdAndUserId(testAccountId, testUserId) }
                .isInstanceOf(ResourceNotFoundException::class.java)
                .hasMessage("Account not found or access denied for account id: $testAccountId")
        }
    }
}