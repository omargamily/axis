package com.axis.pay.controller

import com.axis.pay.controller.dto.OpenAccountResponseDto
import com.axis.pay.model.User
import com.axis.pay.service.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService
) {

    @PostMapping("/open")
    fun openAccount(authentication:Authentication): ResponseEntity<OpenAccountResponseDto> {
        val userId = (authentication.principal as User).id ?: throw IllegalStateException("User ID not found in principal")
        val accountId = accountService.openAccount(userId)
        return ResponseEntity.ok(OpenAccountResponseDto(accountId))
    }
}