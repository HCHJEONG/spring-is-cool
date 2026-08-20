package com.hchjeong.springiscool.world

import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import org.springframework.stereotype.Component

@Component
class WorldCommandHandler(
    private val parser: CommandParser,
) {
    fun handle(session: WorldSession, input: String): CommandResult {
        return when (val command = parser.parse(input)) {
            WorldCommand.Empty -> CommandResult.Continue(emptyScene())
            WorldCommand.Look -> CommandResult.Continue(lookScene(session))
            WorldCommand.Answer -> CommandResult.Continue(answerScene(session))
            WorldCommand.Help -> CommandResult.Continue(helpScene())
            WorldCommand.Quit -> CommandResult.Quit(goodbyeScene())
            is WorldCommand.Unknown -> CommandResult.Continue(unknownScene(command.text))
        }
    }

    private fun emptyScene(): Scene {
        return Scene(
            lines = listOf(
                line("The cursor waits.", SceneStyle.MUTED, delayAfterMillis = 220),
            ),
        )
    }

    private fun lookScene(session: WorldSession): Scene {
        val telephoneLine = if (session.telephoneRinging) {
            "The telephone is still ringing."
        } else {
            "The telephone rests in its cradle of silence."
        }

        val lineState = if (session.lineAnswered) {
            "The open line breathes faintly."
        } else {
            "No one has answered."
        }

        return Scene(
            lines = listOf(
                line("The office waits in green phosphor silence.", delayAfterMillis = 420),
                blank(180),
                TextArtLibrary.telephone(delayAfterMillis = 350),
                blank(180),
                line(telephoneLine, delayAfterMillis = 360),
                line(lineState, SceneStyle.MUTED, delayAfterMillis = 260),
            ),
        )
    }

    private fun answerScene(session: WorldSession): Scene {
        if (!session.answerTelephone()) {
            return Scene(
                lines = listOf(
                    line("The receiver is already warm in your hand.", delayAfterMillis = 360),
                    blank(160),
                    line("UNKNOWN CALLER:", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                    line("\"I said, you are late.\"", SceneStyle.DIALOGUE, delayAfterMillis = 420),
                ),
            )
        }

        return Scene(
            lines = listOf(
                line("You lift the receiver.", delayAfterMillis = 500),
                blank(220),
                line("CLICK.", SceneStyle.SIGNAL, RevealMode.INSTANT, 650, alignment = SceneAlignment.CENTER),
                blank(220),
                line("A carrier tone breathes on the line.", delayAfterMillis = 520),
                blank(180),
                line("UNKNOWN CALLER:", SceneStyle.SYSTEM, RevealMode.INSTANT, 250),
                line("\"You are late.\"", SceneStyle.DIALOGUE, delayAfterMillis = 600),
            ),
        )
    }

    private fun helpScene(): Scene {
        return Scene(
            lines = listOf(
                line("COMMAND CHANNEL", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line("  LOOK     inspect the room", SceneStyle.MUTED, RevealMode.INSTANT, 120),
                line("  ANSWER   pick up the telephone", SceneStyle.MUTED, RevealMode.INSTANT, 120),
                line("  HELP     show this list", SceneStyle.MUTED, RevealMode.INSTANT, 120),
                line("  QUIT     close the line", SceneStyle.MUTED, RevealMode.INSTANT, 220),
            ),
        )
    }

    private fun unknownScene(input: String): Scene {
        return Scene(
            lines = listOf(
                line("The system does not recognize `$input`.", SceneStyle.WARNING, delayAfterMillis = 360),
                line("Type HELP if the room feels too quiet.", SceneStyle.MUTED, delayAfterMillis = 240),
            ),
        )
    }

    private fun goodbyeScene(): Scene {
        return Scene(
            showPromptAfter = false,
            lines = listOf(
                line("The line goes dead.", SceneStyle.SYSTEM, delayAfterMillis = 220),
            ),
        )
    }

    private fun blank(delayAfterMillis: Long): SceneLine {
        return line("", delayAfterMillis = delayAfterMillis)
    }

    private fun line(
        text: String,
        style: SceneStyle = SceneStyle.NARRATION,
        reveal: RevealMode = RevealMode.TYPEWRITER,
        delayAfterMillis: Long,
        characterDelayMillis: Long = 28,
        alignment: SceneAlignment = SceneAlignment.LEFT,
    ): SceneLine {
        return SceneLine(
            text = text,
            reveal = reveal,
            style = style,
            delayAfterMillis = delayAfterMillis,
            characterDelayMillis = characterDelayMillis,
            alignment = alignment,
        )
    }
}

sealed interface CommandResult {
    val scene: Scene

    data class Continue(override val scene: Scene) : CommandResult
    data class Quit(override val scene: Scene) : CommandResult
}
