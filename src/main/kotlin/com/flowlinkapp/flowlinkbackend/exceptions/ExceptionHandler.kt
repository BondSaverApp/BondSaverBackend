package com.flowlinkapp.flowlinkbackend.exceptions

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class RestException(
  val code: Int,
  val type: String,
  val message: String?,
)

@RestControllerAdvice
class ExceptionHandlerAdvice{
  @ExceptionHandler(Exception::class)
  fun handle(e: Exception): ResponseEntity<RestException> {
    if (e is ServerException) {
      when (e) {
        is InternalServerException -> {
          return ResponseEntity.status(e.statusCode()).body(RestException(
            e.statusCode(),
            e.name(),
            e.cause?.message // todo: remove, send just null, otherwise it is unsafe!
          ))
        }
        else -> {
          return ResponseEntity.status(e.statusCode()).body(RestException(
            e.statusCode(),
            e.name(),
            e.message
          ))
        }
      }
    }
    else {
      return ResponseEntity.status(InternalServerException().statusCode()).body(RestException(
        InternalServerException().statusCode(),
        InternalServerException().name(),
        null
      ))
    }
  }
}