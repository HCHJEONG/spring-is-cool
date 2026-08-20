package com.hchjeong.springiscool.world

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class PresenceRegistry {
    private val activeSshUsers = AtomicInteger(0)

    fun enterSsh(): PresenceLease {
        activeSshUsers.incrementAndGet()
        return PresenceLease {
            activeSshUsers.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
        }
    }

    fun snapshot(): PresenceSnapshot {
        return PresenceSnapshot(activeUserCount = activeSshUsers.get())
    }
}

fun interface PresenceLease {
    fun close()
}

data class PresenceSnapshot(
    val activeUserCount: Int = 0,
    val userRole: ParticipantRole = ParticipantRole(
        displayName = "SSH user",
        authority = listOf(
            "inspect office",
            "answer incoming line",
            "request AI scene",
            "delegate approved tasks",
            "read event log",
        ),
    ),
    val agents: List<WorldAgentPresence> = listOf(
        WorldAgentPresence(
            id = "ai-clerk",
            displayName = "AI clerk",
            state = "STANDBY",
            authority = listOf(
                "check line state",
                "describe office state",
                "attach line evidence",
            ),
        ),
    ),
)

data class ParticipantRole(
    val displayName: String,
    val authority: List<String>,
)

data class WorldAgentPresence(
    val id: String,
    val displayName: String,
    val state: String,
    val authority: List<String>,
)
