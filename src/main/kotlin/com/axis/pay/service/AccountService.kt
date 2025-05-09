package com.axis.pay.service

import com.axis.pay.model.Account
import com.axis.pay.model.repository.AccountRepository
import com.axis.pay.model.repository.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) {

    fun openAccount(userId: UUID): UUID {
        val user = userRepository.findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found with id: $userId") }

        val newAccount = Account(user = user)
        val savedAccount = accountRepository.save(newAccount)
        return savedAccount.id
    }
}