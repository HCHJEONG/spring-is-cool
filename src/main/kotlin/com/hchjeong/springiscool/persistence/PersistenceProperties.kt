package com.hchjeong.springiscool.persistence

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring-is-cool.persistence")
data class PersistenceProperties(
    val enabled: Boolean = true,
    val type: StoreType = StoreType.SQLITE,
    val sqlitePath: String = "build/data/world.sqlite",
    val sessionId: String = "default-office",
)

enum class StoreType {
    SQLITE,
}
