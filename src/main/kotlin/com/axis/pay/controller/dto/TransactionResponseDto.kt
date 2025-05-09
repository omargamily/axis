package com.axis.pay.controller.dto

import com.axis.pay.model.TransactionType
import java.util.UUID

data class TransactionResponseDto(
    val id: UUID,
    val accountId: UUID,
    val type: TransactionType,
    val amount: Double,
    val timestamp: Long,
    val currentBalance: Double
)