package com.flowlinkapp.flowlinkbackend.config

import com.flowlinkapp.flowlinkbackend.auth.JwtTokenProvider
import com.flowlinkapp.flowlinkbackend.filters.JwtAuthenticationFilter
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
      .securityMatcher("/api/{auth:(?!auth)(?:[a-z0-9]+)}/**")
      .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)
      .authorizeHttpRequests {
        it.requestMatchers("/api/auth/**").permitAll()
        it.anyRequest().authenticated()
      }
    return http.build()
  }
}