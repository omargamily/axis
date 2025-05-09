package com.axis.pay.service

import com.axis.pay.BadRequestException
import com.axis.pay.ResourceConflictException
import com.axis.pay.ResourceNotFoundException
import com.axis.pay.UnAuthorizedException
import com.axis.pay.model.Account
import com.axis.pay.model.Transaction
import com.axis.pay.model.TransactionType
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.TransactionRepository
import com.axis.pay.model.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {

    fun openAccount(userId: UUID): UUID {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UsernameNotFoundException("User not found with id: $userId")

        val newAccount = Account(user = user)
        val savedAccount = accountRepository.save(newAccount)
        return savedAccount.id
    }

    @Transactional
    fun deposit(accountId: UUID, amount: Double, userId: UUID): Transaction {
        if (amount <= 0) {
            throw IllegalArgumentException("Deposit amount must be positive")
        }
        val account = accountRepository.findByIdOrNull(accountId)
            ?: throw ResourceNotFoundException("Account not found with id: $accountId")

        if (account.user.id != userId) {
            throw UnAuthorizedException("User not authorized to deposit to this account")
        }

        account.balance += amount
        val savedAccount = accountRepository.save(account)

        val transaction = Transaction(
            account = savedAccount,
            type = TransactionType.DEPOSIT,
            amount = amount
        )
        return transactionRepository.save(transaction)
    }

    @Transactional
    fun withdraw(accountId: UUID, amount: Double, userId: UUID): Transaction {
        if (amount <= 0) {
            throw IllegalArgumentException("Withdrawal amount must be positive")
        }
        val account = accountRepository.findByIdOrNull(accountId)
            ?: throw ResourceNotFoundException("Account not found with id: $accountId")

        if (account.user.id != userId) {
            throw UnAuthorizedException("User not authorized to withdraw from this account")
        }

        if (account.balance < amount) {
            throw BadRequestException("Insufficient funds for withdrawal. Current balance: ${account.balance}")
        }

        account.balance -= amount
        val savedAccount = accountRepository.save(account)

        val transaction = Transaction(
            account = savedAccount,
            type = TransactionType.WITHDRAW,
            amount = amount
        )
        return transactionRepository.save(transaction)
    }

    fun getBalance(accountId: UUID, userId: UUID): Double {
        val account = accountRepository.findByIdOrNull(accountId)
            ?: throw ResourceNotFoundException("Account not found with id: $accountId")

        if (account.user.id != userId) {
            throw UnAuthorizedException("User not authorized to view this account's balance")
        }
        return account.balance
    }

    fun findAccountByIdAndUserId(accountId: UUID, userId: UUID): Account {
        return accountRepository.findById(accountId)
            .filter { it.user.id == userId }
            .orElseThrow { ResourceNotFoundException("Account not found or access denied for account id: $accountId") }
    }
}