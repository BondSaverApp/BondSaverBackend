package com.uiop07558.socnet.filters

import com.uiop07558.socnet.auth.JwtTokenProvider
import com.uiop07558.socnet.auth.model.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class CustomUserAuthentication(
  private val userId: String,
  private val roles: List<UserRole>,
  private val authenticated: Boolean = true
) : Authentication {
  override fun getPrincipal(): String = userId
  fun getRoles(): List<UserRole> = roles

  override fun getCredentials(): Any? = null

  override fun getAuthorities(): Collection<GrantedAuthority> =
    roles.map { SimpleGrantedAuthority("ROLE_${it.name.uppercase()  }") }

  override fun isAuthenticated(): Boolean = authenticated
  override fun setAuthenticated(authenticated: Boolean) {
    throw UnsupportedOperationException("Cannot change authentication status")
  }

  override fun getDetails(): CustomUserDetails =
    CustomUserDetails(userId, roles)

  override fun getName(): String = userId
}

// Helper class to store our custom user details
data class CustomUserDetails(
  val userId: String,
  val roles: List<UserRole>
)

class JwtAuthenticationFilter(
  private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain
  ) {
    try {
      val jwt = getJwtFromRequest(request)
      if (jwt == null) return
      val claims = jwtTokenProvider.validateAndParseAccessToken(jwt)
      if (claims == null) return

      val userId = claims.subject
      val roles = claims.audience
      val authentication = CustomUserAuthentication(userId, roles)

      SecurityContextHolder.getContext().authentication = authentication
    } catch (ex: Exception) {
      logger.error("Could not set user authentication in security context", ex)
      return
    }

    filterChain.doFilter(request, response)
  }

  private fun getJwtFromRequest(request: HttpServletRequest): String? {
    val bearerToken = request.getHeader("Authorization")
    if (bearerToken?.startsWith("Bearer ") == true) {
      return bearerToken.substring(7)
    }
    return null
  }
}
