package com.flowlinkapp.flowlinkbackend.auth

import com.flowlinkapp.flowlinkbackend.auth.config.AuthProperties
import com.flowlinkapp.flowlinkbackend.auth.model.User
import com.flowlinkapp.flowlinkbackend.auth.model.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.*

data class AccessClaims(
  val subject: String,
  val audience: List<UserRole>,
  val sessionId: String,
  val issuedAt: Instant,
  val expiresAt: Instant,
)

data class RefreshClaims(
  val subject: String,
  val sessionId: String,
  val issuedAt: Instant,
  val expiresAt: Instant,
)

@Component
class JwtTokenProvider(
  private val authProperties: AuthProperties,
) {
  fun generateAccessToken(user: User, sessionId: String): String {
    return generateAccessToken(user, sessionId, authProperties.accessTokenExpiration)
  }

  fun generateRefreshToken(user: User, sessionId: String,): String {
    return generateRefreshToken(user, sessionId, authProperties.refreshTokenExpiration)
  }

  private fun generateAccessToken(user: User, sessionId: String, expirationSec: Long): String {
    val now = Date()
    val expiration = Date(now.time + (expirationSec.toLong() * 1000))

    val key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(authProperties.secret))

    return Jwts.builder()
      .subject(user.id.toString())
      .audience().add(user.roles.map { it.name }).and()
      .claim("sid", sessionId)
      .issuedAt(now)
      .expiration(expiration)
      .signWith(key)
      .compact()
  }

  private fun generateRefreshToken(user: User, sessionId: String, expirationSec: Long): String {
    val now = Date()
    val expiration = Date(now.time + (expirationSec.toLong() * 1000))

    val key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(authProperties.secret))
    return Jwts.builder()
      .subject(user.id.toString())
      .claim("sid", sessionId)
      .issuedAt(now)
      .expiration(expiration)
      .signWith(key)
      .compact()
  }

  fun validateAndParseAccessToken(token: String): AccessClaims? {
    val key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(authProperties.secret))
    try {
      val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
      if (claims.expiration.before(Date())) return null

      return AccessClaims(
        subject = claims.subject,
        audience = claims.audience.toList().map { UserRole.valueOf(it) },
        sessionId = claims.get("sid", String::class.java),
        issuedAt = claims.issuedAt.toInstant().toKotlinInstant(),
        expiresAt = claims.expiration.toInstant().toKotlinInstant(),
      )
    } catch (e: Exception) {
      return null
    }
  }

  fun validateAndParseRefreshToken(token: String): RefreshClaims? {
    val key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(authProperties.secret))
    try {
      val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
      if (claims.expiration.before(Date())) return null

      return RefreshClaims(
        subject = claims.subject,
        sessionId = claims.get("sid", String::class.java),
        issuedAt = claims.issuedAt.toInstant().toKotlinInstant(),
        expiresAt = claims.expiration.toInstant().toKotlinInstant(),
      )
    } catch (e: Exception) {
      return null
    }
  }
}