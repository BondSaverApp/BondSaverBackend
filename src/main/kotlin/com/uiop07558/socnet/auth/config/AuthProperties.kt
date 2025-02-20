package com.uiop07558.socnet.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("service.auth")
class AuthProperties(
  val accessTokenExpiration: Long = 0,
  val refreshTokenExpiration: Long = 0,
)