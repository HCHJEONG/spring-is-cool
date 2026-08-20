package com.hchjeong.springiscool.world

import org.springframework.stereotype.Component

@Component
class CommandParser {
    fun parse(input: String): WorldCommand {
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            return WorldCommand.Empty
        }

        return when (normalized.uppercase()) {
            "LOOK", "L" -> WorldCommand.Look
            "ANSWER", "A" -> WorldCommand.Answer
            "HELP", "?" -> WorldCommand.Help
            "QUIT", "EXIT" -> WorldCommand.Quit
            else -> WorldCommand.Unknown(normalized)
        }
    }
}

sealed interface WorldCommand {
    data object Empty : WorldCommand
    data object Look : WorldCommand
    data object Answer : WorldCommand
    data object Help : WorldCommand
    data object Quit : WorldCommand
    data class Unknown(val text: String) : WorldCommand
}
