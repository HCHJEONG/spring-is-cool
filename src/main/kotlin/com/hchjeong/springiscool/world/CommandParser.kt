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

        return when (normalized.uppercase()) {
            "LOOK", "L" -> WorldCommand.Look
            "ANSWER", "A" -> WorldCommand.Answer
            "LOG", "REMEMBER", "HISTORY" -> WorldCommand.Log
            "STATUS", "STAT" -> WorldCommand.Status
            "HELP", "?" -> WorldCommand.Help
            "QUIT", "EXIT" -> WorldCommand.Quit
            "AI" -> WorldCommand.Ai("")
            else -> WorldCommand.Unknown(normalized)
        }
    }

    private fun parseAiCommand(input: String): WorldCommand? {
        val marker = "AI "
        return if (input.length > marker.length && input.regionMatches(0, marker, 0, marker.length, ignoreCase = true)) {
            WorldCommand.Ai(input.drop(marker.length).trim())
        } else {
            null
        }
    }
}

sealed interface WorldCommand {
    data object Empty : WorldCommand
    data object Look : WorldCommand
    data object Answer : WorldCommand
    data object Log : WorldCommand
    data object Status : WorldCommand
    data object Help : WorldCommand
    data object Quit : WorldCommand
    data class Ai(val text: String) : WorldCommand
    data class Unknown(val text: String) : WorldCommand
}
