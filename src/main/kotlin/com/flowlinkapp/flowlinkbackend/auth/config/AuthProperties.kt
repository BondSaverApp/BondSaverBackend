package com.flowlinkapp.flowlinkbackend.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties("service.auth")
class AuthProperties(
  val accessTokenExpiration: Long = 0,
  val refreshTokenExpiration: Long = 0,
  val secret: String = "pns"
)

@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class AuthPropertiesConfig