package com.esop

import io.micronaut.http.HttpStatus

open class HttpException(val status: HttpStatus, message: String) : RuntimeException(message)

class PlatformFeeLessThanZeroException : Exception("Platform fee cannot be less than zero")