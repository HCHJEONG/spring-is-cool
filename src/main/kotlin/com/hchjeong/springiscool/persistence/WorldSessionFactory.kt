package com.hchjeong.springiscool.persistence

import com.hchjeong.springiscool.world.WorldSession
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class WorldSessionFactory(
    private val eventStore: WorldEventStore,
    private val properties: PersistenceProperties,
) {
    fun create(): WorldSession {
        val sessionId = properties.sessionId.ifBlank { UUID.randomUUID().toString() }
        return WorldSession(
            sessionId = sessionId,
            eventStore = eventStore,
            storedEvents = eventStore.load(sessionId),
        )
    }
}
