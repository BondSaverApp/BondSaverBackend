package com.flowlinkapp.flowlinkbackend.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig(
  @Value("\${s3.host}")
  val host: String,
  @Value("\${s3.accessKey}")
  val accessKey: String,
  @Value("\${s3.secretkey}")
  val secretkey: String,
) {
  @Bean
  fun minioClient(): MinioClient {
    return MinioClient.builder()
      .endpoint(host)
      .credentials(accessKey, secretkey)
      .build()
  }
}