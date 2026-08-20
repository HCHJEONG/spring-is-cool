package com.hchjeong.springiscool.world

import java.time.Instant

class WorldSession {
    val actor: Actor = Actor("operator")
    private val events = mutableListOf<WorldEvent>()

    var telephoneRinging: Boolean = true
        private set

    var lineAnswered: Boolean = false
        private set

    init {
        record(
            action = WorldAction.SystemOpened,
            observation = "Ontoloffice opened a line into the empty office.",
        )
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

    data class UnknownCommand(val text: String) : WorldAction {
        override val verb = "UNKNOWN"
    }
}
