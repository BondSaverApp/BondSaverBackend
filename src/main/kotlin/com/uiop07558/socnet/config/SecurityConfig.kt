package com.uiop07558.socnet.config

import com.uiop07558.socnet.auth.JwtTokenProvider
import com.uiop07558.socnet.filters.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
  private val jwtTokenProvider: JwtTokenProvider,
) {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .csrf { it.disable() }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .authorizeHttpRequests {
        it.requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
        it.anyRequest().authenticated()
      }
      .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)
    return http.build()
  }
}