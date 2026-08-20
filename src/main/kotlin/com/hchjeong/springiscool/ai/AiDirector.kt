package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.scene.SceneDefinition
import com.hchjeong.springiscool.world.PresenceSnapshot
import com.hchjeong.springiscool.world.WorldSession
import com.hchjeong.springiscool.world.WorldProjection

interface AiDirector {
    fun direct(request: AiDirectorRequest): AiDirectorResult
}

data class AiDirectorRequest(
    val userText: String,
    val worldSummary: WorldSummary,
    val availableTextArt: List<String>,
)

data class WorldSummary(
    val actorId: String,
    val telephoneRinging: Boolean,
    val lineAnswered: Boolean,
    val lineOffline: Boolean,
    val facts: List<String>,
    val eventCount: Int,
    val recentEvents: List<String>,
) {
    companion object {
        fun from(session: WorldSession, presence: PresenceSnapshot = PresenceSnapshot(), maxEvents: Int = 6): WorldSummary {
            val projection = WorldProjection.from(session, presence)
            return WorldSummary(
                actorId = session.actor.id,
                telephoneRinging = session.telephoneRinging,
                lineAnswered = session.lineAnswered,
                lineOffline = session.lineOffline,
                facts = projection.facts,
                eventCount = session.history().size,
                recentEvents = session.history().takeLast(maxEvents).map {
                    "${it.sequence.toString().padStart(3, '0')} ${it.action.verb}: ${it.observation.text}"
                },
            )
        }
    }
}

sealed interface AiDirectorResult {
    val scene: Scene

    data class Generated(
        override val scene: Scene,
        val definition: SceneDefinition,
        val reviewNotes: List<String>,
    ) : AiDirectorResult

    data class Fallback(
        override val scene: Scene,
        val reason: String,
    ) : AiDirectorResult
}
