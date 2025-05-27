package com.flowlinkapp.flowlinkbackend.exceptions

sealed class ServerException(
  override val message: String?,
  override val cause: Throwable?,
): Exception(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  abstract fun statusCode(): Int
  abstract fun name(): String
}

class NotFoundServerException(
  override val message: String?,
  override val cause: Throwable?,
): ServerException(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  override fun statusCode(): Int {
    return 404
  }
  override fun name(): String {
    return "Not found"
  }
}

class BadRequestServerException(
  override val message: String?,
  override val cause: Throwable?,
): ServerException(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  override fun statusCode(): Int {
    return 400
  }
  override fun name(): String {
    return "Bad request"
  }
}

class UnauthenticatedServerException(
  override val message: String?,
  override val cause: Throwable?,
): ServerException(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  override fun statusCode(): Int {
    return 401
  }
  override fun name(): String {
    return "Unauthenticated"
  }
}

class UnauthorizedServerException(
  override val message: String?,
  override val cause: Throwable?,
): ServerException(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  override fun statusCode(): Int {
    return 403
  }
  override fun name(): String {
    return "Unauthorized"
  }
}

class InternalServerException(
  override val message: String?,
  override val cause: Throwable?,
): ServerException(message, cause) {
  constructor(): this(null, null)
  constructor(message: String?): this(message, null)
  constructor(cause: Throwable?): this(null, cause)

  override fun statusCode(): Int {
    return 500
  }
  override fun name(): String {
    return "Internal server error"
  }
}