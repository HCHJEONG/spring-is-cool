package com.hchjeong.springiscool.persistence

import com.hchjeong.springiscool.world.WorldEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

interface WorldEventStore {
    fun append(sessionId: String, event: WorldEvent)
    fun load(sessionId: String): List<StoredWorldEvent>
}

data class StoredWorldEvent(
    val sessionId: String,
    val sequence: Int,
    val occurredAt: Instant,
    val actorId: String,
    val actionVerb: String,
    val actionText: String?,
    val observationText: String,
)

@Component
@ConditionalOnProperty(
    prefix = "spring-is-cool.persistence",
    name = ["enabled"],
    havingValue = "false",
)
class DisabledWorldEventStore : WorldEventStore {
    override fun append(sessionId: String, event: WorldEvent) {
    }

    override fun load(sessionId: String): List<StoredWorldEvent> {
        return emptyList()
    }
}
