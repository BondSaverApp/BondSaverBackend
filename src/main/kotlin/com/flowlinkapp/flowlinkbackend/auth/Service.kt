package com.flowlinkapp.flowlinkbackend.auth

import com.flowlinkapp.flowlinkbackend.auth.config.AuthProperties
import com.flowlinkapp.flowlinkbackend.auth.model.Session
import com.flowlinkapp.flowlinkbackend.auth.model.User
import com.flowlinkapp.flowlinkbackend.auth.model.UserRole
import com.flowlinkapp.flowlinkbackend.auth.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class LoginRequest(
  val phoneNumber: String,
  val password: String,
)

data class SignupRequest(
  val phoneNumber: String,
  val password: String,
  val email: String?,
  val username: String,
)

data class CheckAccountRequest(
  val phoneNumber: String,
)

data class CheckAccountResponse(
  val exists: Boolean,
)

data class AuthResponse(
  val accessToken: String,
  val accessTokenDuration: Duration,
  val refreshToken: String,
  val refreshTokenDuration: Duration,
  val tokenType: String
)

@Service
class AuthService(
  private val authProperties: AuthProperties,

  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
  private val jwtTokenProvider: JwtTokenProvider,
) {
  fun checkAccount(request: CheckAccountRequest): CheckAccountResponse {
    return CheckAccountResponse(userRepository.existsByPhone(request.phoneNumber))
  }

  fun signup(signupRequest: SignupRequest): AuthResponse {
    if (userRepository.existsByPhone(signupRequest.phoneNumber)) {
      throw RuntimeException("Phone is already in use")
    }

    val user = User(
      id = ObjectId(),
      phone = signupRequest.phoneNumber,
      email = signupRequest.email,
      username = signupRequest.username,
      roles = mutableListOf(UserRole.User),
      passwordHash = passwordEncoder.encode(signupRequest.password),
      sessions = mutableListOf<Session>()
    )

    val session = Session(authProperties.refreshTokenExpiration)

    val accessToken = jwtTokenProvider.generateAccessToken(user, session.id.toString())
    val refreshToken = jwtTokenProvider.generateRefreshToken(user, session.id.toString())

    user.sessions.add(session)

    userRepository.save(user)

    return AuthResponse(
      accessToken = accessToken,
      accessTokenDuration = authProperties.accessTokenExpiration.toDuration(DurationUnit.SECONDS),
      refreshToken = refreshToken,
      refreshTokenDuration = authProperties.refreshTokenExpiration.toDuration(DurationUnit.SECONDS),
      tokenType = "Bearer"
    )
  }

  fun login(loginRequest: LoginRequest): AuthResponse {
    var user = userRepository.findByPhone(loginRequest.phoneNumber)
      ?: throw RuntimeException("User not found")


    if (!passwordEncoder.matches(loginRequest.password, user.passwordHash)) {
      throw RuntimeException("Invalid password")
    }

    val session = Session(authProperties.refreshTokenExpiration)

    val accessToken = jwtTokenProvider.generateAccessToken(user, session.id.toString())
    val refreshToken = jwtTokenProvider.generateRefreshToken(user, session.id.toString())

    user.addSession(session)

    userRepository.save(user)

    return AuthResponse(
      accessToken = accessToken,
      accessTokenDuration = authProperties.accessTokenExpiration.toDuration(DurationUnit.SECONDS),
      refreshToken = refreshToken,
      refreshTokenDuration = authProperties.refreshTokenExpiration.toDuration(DurationUnit.SECONDS),
      tokenType = "Bearer"
    )
  }

  fun refresh(refreshToken: String): AuthResponse {
    val refreshClaims = jwtTokenProvider.validateAndParseRefreshToken(refreshToken)
    if (refreshClaims == null) {
      throw RuntimeException("Invalid refresh token")
    }

    var user = userRepository.findById(ObjectId(refreshClaims.subject)).orElse(null)
      ?: throw RuntimeException("User not found")

    user.checkAndRemoveSession(ObjectId(refreshClaims.sessionId))

    val newSession = Session(authProperties.refreshTokenExpiration)

    val newAccessToken = jwtTokenProvider.generateAccessToken(user, newSession.id.toString())
    val newRefreshToken = jwtTokenProvider.generateRefreshToken(user, newSession.id.toString())

    user.addSession(newSession)

    userRepository.save(user)

    return AuthResponse(
      accessToken = newAccessToken,
      accessTokenDuration = authProperties.accessTokenExpiration.toDuration(DurationUnit.SECONDS),
      refreshToken = newRefreshToken,
      refreshTokenDuration = authProperties.refreshTokenExpiration.toDuration(DurationUnit.SECONDS),
      tokenType = "Bearer"
    )
  }

  fun logout(refreshToken: String) {
    val refreshClaims = jwtTokenProvider.validateAndParseRefreshToken(refreshToken)
    if (refreshClaims == null) {
      throw RuntimeException("Invalid refresh token")
    }

    var user = userRepository.findById(ObjectId(refreshClaims.subject)).orElse(null)
      ?: throw RuntimeException("User not found")

    user.checkAndRemoveSession(ObjectId(refreshClaims.sessionId))

    userRepository.save(user)
  }
}