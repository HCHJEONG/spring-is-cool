package com.hchjeong.springiscool.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring-is-cool.ai")
data class AiDirectorProperties(
    val enabled: Boolean = false,
    val provider: String = "static",
    val project: String = "",
    val location: String = "global",
    val modelId: String = "gemini-2.5-flash-lite",
    val timeoutMillis: Long = 15_000,
)
