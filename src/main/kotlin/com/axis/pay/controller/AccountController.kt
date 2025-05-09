package com.axis.pay.controller

import com.axis.pay.ResourceNotFoundException
import com.axis.pay.controller.dto.BalanceResponseDto
import com.axis.pay.controller.dto.OpenAccountResponseDto
import com.axis.pay.controller.dto.TransactionRequestDto
import com.axis.pay.controller.dto.TransactionResponseDto
import com.axis.pay.model.User
import com.axis.pay.service.AccountService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @PostMapping("/open")
    fun openAccount(authentication: Authentication): ResponseEntity<OpenAccountResponseDto> {
        val user = authentication.principal as User
        val userId = user.id ?: throw ResourceNotFoundException("User ID not found in principal")
        val accountId = accountService.openAccount(userId)
        return ResponseEntity.ok(OpenAccountResponseDto(accountId))
    }

    @PostMapping("/{accountId}/deposit")
    fun deposit(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: TransactionRequestDto,
        authentication: Authentication
    ): TransactionResponseDto {
        val user = authentication.principal as User
        val userId = user.id ?: throw ResourceNotFoundException("User not found")
        val transaction = accountService.deposit(accountId, request.amount, userId)
        val account = accountService.findAccountByIdAndUserId(accountId, userId)
        return TransactionResponseDto(
            id = transaction.id,
            accountId = transaction.account.id,
            type = transaction.type,
            amount = transaction.amount,
            timestamp = transaction.timestamp,
            currentBalance = account.balance
        )
    }

    @PostMapping("/{accountId}/withdraw")
    fun withdraw(
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: TransactionRequestDto,
        authentication: Authentication
    ): TransactionResponseDto {
        val user = authentication.principal as User
        val userId = user.id ?: throw ResourceNotFoundException("User not found")
        val transaction = accountService.withdraw(accountId, request.amount, userId)
        val account = accountService.findAccountByIdAndUserId(accountId, userId)
        return TransactionResponseDto(
            id = transaction.id,
            accountId = transaction.account.id,
            type = transaction.type,
            amount = transaction.amount,
            timestamp = transaction.timestamp,
            currentBalance = account.balance
        )
    }

    @GetMapping("/{accountId}/balance")
    fun getBalance(
        @PathVariable accountId: UUID,
        authentication: Authentication
    ): BalanceResponseDto {
        val user = authentication.principal as User
        val userId = user.id ?: throw ResourceNotFoundException("User not found")
        val balance = accountService.getBalance(accountId, userId)
        return BalanceResponseDto(accountId = accountId, balance = balance)
    }
}