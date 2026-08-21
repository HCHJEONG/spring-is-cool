package com.hchjeong.springiscool.world

import org.springframework.stereotype.Component

@Component
class CommandParser {
    fun parse(input: String): WorldCommand {
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            return WorldCommand.Empty
        }

        parseAiCommand(normalized)?.let { return it }
        parseAssignCommand(normalized)?.let { return it }
        parseLogCommand(normalized)?.let { return it }

        return when (normalized.uppercase()) {
            "LOOK", "L" -> WorldCommand.Look
            "ANSWER", "A" -> WorldCommand.Answer
            "STATUS", "STAT" -> WorldCommand.Status
            "HELP", "?" -> WorldCommand.Help
            "QUIT", "EXIT" -> WorldCommand.Quit
            "AI" -> WorldCommand.Ai("")
            "ASSIGN" -> WorldCommand.Assign("", "")
            else -> WorldCommand.Unknown(normalized)
        }
    }

    private fun parseLogCommand(input: String): WorldCommand? {
        val match = Regex("^(LOG|REMEMBER|HISTORY)(?:\\s+(?:FOR\\s+)?(\\d+))?$", RegexOption.IGNORE_CASE)
            .matchEntire(input)
            ?: return null

        return WorldCommand.Log(match.groupValues.getOrNull(2)?.toIntOrNull() ?: WorldCommand.DEFAULT_LOG_LIMIT)
    }

    private fun parseAiCommand(input: String): WorldCommand? {
        val marker = "AI "
        return if (input.length > marker.length && input.regionMatches(0, marker, 0, marker.length, ignoreCase = true)) {
            WorldCommand.Ai(input.drop(marker.length).trim())
        } else {
            null
        }
    }

    private fun parseAssignCommand(input: String): WorldCommand? {
        val marker = "ASSIGN "
        if (input.length <= marker.length || !input.regionMatches(0, marker, 0, marker.length, ignoreCase = true)) {
            return null
        }

        val payload = input.drop(marker.length).trim()
        val aiClerkMarker = "AI clerk "
        if (payload.length > aiClerkMarker.length &&
            payload.regionMatches(0, aiClerkMarker, 0, aiClerkMarker.length, ignoreCase = true)
        ) {
            return WorldCommand.Assign("ai-clerk", payload.drop(aiClerkMarker.length).trim())
        }

        val parts = payload.split(Regex("\\s+"), limit = 2)
        val agentId = parts.getOrElse(0) { "" }
        val task = parts.getOrElse(1) { "" }
        return WorldCommand.Assign(agentId, task)
    }
}

sealed interface WorldCommand {
    data object Empty : WorldCommand
    data object Look : WorldCommand
    data object Answer : WorldCommand
    data class Log(val limit: Int = DEFAULT_LOG_LIMIT) : WorldCommand
    data object Status : WorldCommand
    data object Help : WorldCommand
    data object Quit : WorldCommand
    data class Ai(val text: String) : WorldCommand
    data class Assign(val agentId: String, val task: String) : WorldCommand
    data class Unknown(val text: String) : WorldCommand

    companion object {
        const val DEFAULT_LOG_LIMIT = 8
    }
}
