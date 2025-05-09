package com.axis.pay.controller.dto

import java.util.UUID

data class BalanceResponseDto(
    val accountId: UUID,
    val balance: Double
)