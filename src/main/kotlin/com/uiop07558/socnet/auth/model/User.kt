package com.uiop07558.socnet.auth.model

import kotlinx.datetime.Clock
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import kotlinx.datetime.Instant
import org.springframework.data.annotation.Id
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Document
class Session(
  @Id
  val id: ObjectId,
  val expiresAt: Instant
) {
  constructor(expiresInMs: Long) : this(id = ObjectId(), expiresAt = Clock.System.now() + expiresInMs.toDuration(DurationUnit.MILLISECONDS))
}

enum class UserRole(val role: String) {
  User("User")
}

@Document(collection = "users")
class User(
  @Id
  val id: ObjectId,
  val phoneNumber: String,
  val email: String?,
  val username: String,
  val roles: MutableList<UserRole>,
  val passwordHash: String,
  val sessions: MutableList<Session>
) {
  fun addSession(session: Session) {
    this.sessions.add(session)
  }

  fun checkAndRemoveSession(sessionId: ObjectId) {
    val oldSessIndex = this.sessions.indexOfFirst { it.id == sessionId }
    if (oldSessIndex == -1) {
      throw RuntimeException("Invalid refresh token")
    }
    this.sessions.removeAt(oldSessIndex)
  }
}