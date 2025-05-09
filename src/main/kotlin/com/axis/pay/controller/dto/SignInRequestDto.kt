package com.axis.pay.controller.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignInRequestDto(
    @field:NotBlank(message = "Email cannot be blank")
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:NotBlank(message = "Password cannot be blank")
    val password: String,
)
