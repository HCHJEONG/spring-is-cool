package com.hchjeong.springiscool.world

import com.hchjeong.springiscool.persistence.DisabledWorldEventStore
import com.hchjeong.springiscool.persistence.StoredWorldEvent
import com.hchjeong.springiscool.persistence.WorldEventStore
import java.time.Instant

class WorldSession(
    val sessionId: String = "local",
    private val eventStore: WorldEventStore = DisabledWorldEventStore(),
    storedEvents: List<StoredWorldEvent> = emptyList(),
) {
    val actor: Actor = Actor("operator")
    private val events = storedEvents.map { it.toWorldEvent() }.toMutableList()

    var telephoneRinging: Boolean = true
        private set

    var lineAnswered: Boolean = false
        private set

    init {
        if (events.any { it.action == WorldAction.AnsweredTelephone }) {
            lineAnswered = true
            telephoneRinging = false
        }

        if (events.isEmpty()) {
            record(
                action = WorldAction.SystemOpened,
                observation = "Ontoloffice opened a line into the empty office.",
            )
        }
    }

    fun history(): List<WorldEvent> {
        return events.toList()
    }

    fun record(action: WorldAction, observation: String): WorldEvent {
        val event = WorldEvent(
            sequence = events.size + 1,
            occurredAt = Instant.now(),
            actor = actor,
            action = action,
            observation = Observation(observation),
        )
        events += event
        eventStore.append(sessionId, event)
        return event
    }

    fun answerTelephone(): Boolean {
        if (lineAnswered) {
            return false
        }

        lineAnswered = true
        telephoneRinging = false
        return true
    }

    private fun StoredWorldEvent.toWorldEvent(): WorldEvent {
        return WorldEvent(
            sequence = sequence,
            occurredAt = occurredAt,
            actor = Actor(actorId),
            action = WorldAction.fromStored(actionVerb, actionText),
            observation = Observation(observationText),
        )
    }
}

data class Actor(
    val id: String,
)

data class Observation(
    val text: String,
)

data class WorldEvent(
    val sequence: Int,
    val occurredAt: Instant,
    val actor: Actor,
    val action: WorldAction,
    val observation: Observation,
)

sealed interface WorldAction {
    val verb: String

    data object SystemOpened : WorldAction {
        override val verb = "SYSTEM"
    }

    data object Looked : WorldAction {
        override val verb = "LOOK"
    }

    data object AnsweredTelephone : WorldAction {
        override val verb = "ANSWER"
    }

    data object RequestedHelp : WorldAction {
        override val verb = "HELP"
    }

    data object RequestedHistory : WorldAction {
        override val verb = "LOG"
    }

    data object CheckedStatus : WorldAction {
        override val verb = "STATUS"
    }

    data object RequestedAiDirector : WorldAction {
        override val verb = "AI"
    }

    data class UnknownCommand(val text: String) : WorldAction {
        override val verb = "UNKNOWN"
    }

    companion object {
        fun fromStored(verb: String, text: String?): WorldAction {
            return when (verb.uppercase()) {
                "SYSTEM" -> SystemOpened
                "LOOK" -> Looked
                "ANSWER" -> AnsweredTelephone
                "HELP" -> RequestedHelp
                "LOG" -> RequestedHistory
                "STATUS" -> CheckedStatus
                "AI" -> RequestedAiDirector
                "UNKNOWN" -> UnknownCommand(text.orEmpty())
                else -> UnknownCommand(text ?: verb)
            }
        }
    }
}
