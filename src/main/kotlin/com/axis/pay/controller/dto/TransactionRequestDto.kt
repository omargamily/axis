package com.axis.pay.controller.dto

import jakarta.validation.constraints.Positive

data class TransactionRequestDto(
    @field:Positive(message = "Amount must be positive")
    val amount: Double
)