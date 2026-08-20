package com.hchjeong.springiscool.adapter.ssh

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring-is-cool.ssh")
data class SshServerProperties(
    val enabled: Boolean = true,
    val host: String = "127.0.0.1",
    val port: Int = 2222,
    val demoUser: String = "demo",
    val demoPassword: String = "demo",
)