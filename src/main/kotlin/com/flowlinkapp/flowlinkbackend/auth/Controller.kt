package com.flowlinkapp.flowlinkbackend.auth

import com.flowlinkapp.flowlinkbackend.auth.config.AuthProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.time.Duration

data class SafeAuthResponse(
  val accessToken: String,
  val accessTokenDuration: Duration,
  val tokenType: String
) {
  constructor(p: AuthResponse): this(p.accessToken, p.accessTokenDuration, p.tokenType)
}

@RestController
@RequestMapping("/api/auth")
class AuthController(
  private val authProperties: AuthProperties,

  private val authService: AuthService
) {
  @PostMapping("/check-account")
  fun checkAccount(@RequestBody request: CheckAccountRequest): ResponseEntity<CheckAccountResponse> {
    val checkResponse = authService.checkAccount(request)
    return ResponseEntity.ok(checkResponse)
  }

  @PostMapping("/signup")
  fun signup(@RequestBody signupRequest: SignupRequest, response: HttpServletResponse): ResponseEntity<SafeAuthResponse> {
    val authResponse = authService.signup(signupRequest)
    setRefreshTokenCookie(response, authResponse.refreshToken, authProperties.refreshTokenExpiration)
    return ResponseEntity.ok(SafeAuthResponse(authResponse))
  }

  @PostMapping("/login")
  fun login(@RequestBody loginRequest: LoginRequest, response: HttpServletResponse): ResponseEntity<SafeAuthResponse> {
    val authResponse = authService.login(loginRequest)
    setRefreshTokenCookie(response, authResponse.refreshToken, authProperties.refreshTokenExpiration)
    return ResponseEntity.ok(SafeAuthResponse(authResponse))
  }

  @PostMapping("/refresh")
  fun refresh(@CookieValue("refreshToken") refreshToken: String, response: HttpServletResponse): ResponseEntity<SafeAuthResponse> {
    val authResponse = authService.refresh(refreshToken)
    setRefreshTokenCookie(response, authResponse.refreshToken, authProperties.refreshTokenExpiration)
    return ResponseEntity.ok(SafeAuthResponse(authResponse))
  }

  @PostMapping("/logout")
  fun logout(response: HttpServletResponse): ResponseEntity<Void> {
    setRefreshTokenCookie(response, "", 0)
    return ResponseEntity.noContent().build()
  }

  private fun setRefreshTokenCookie(response: HttpServletResponse, token: String, expirationSec: Long) {
    val cookie = Cookie("refreshToken", token)
    cookie.isHttpOnly = true
    cookie.secure = true
    cookie.path = "/api/auth"
    cookie.maxAge = expirationSec.toInt()
    response.addCookie(cookie)
  }
}
