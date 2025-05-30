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

data class SafeAuthResponse(
  val accessToken: String,
  val accessTokenDuration: Long,
  val tokenType: String,
  val userId: String,
) {
  constructor(p: AuthResponse): this(p.accessToken, p.accessTokenDuration, p.tokenType, p.userId)
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
    println("Request got: $request, response: $checkResponse")
    return ResponseEntity.ok(checkResponse)
  }

  @PostMapping("/signup")
  fun signup(@RequestBody signupRequest: SignupRequest, response: HttpServletResponse): ResponseEntity<SafeAuthResponse> {
    println("Request got: $signupRequest")
    val authResponse: AuthResponse = authService.signup(signupRequest)
    setRefreshTokenCookie(response, authResponse.refreshToken, authProperties.refreshTokenExpiration)
    println("SignUp auth response: ${SafeAuthResponse(authResponse)}")
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
  fun logout(@CookieValue("refreshToken") refreshToken: String, response: HttpServletResponse): ResponseEntity<Void> {
    authService.logout(refreshToken)
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
