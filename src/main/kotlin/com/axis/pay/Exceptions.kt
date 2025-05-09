package com.axis.pay

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnAuthorizedException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class ResourceConflictException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)