package com.hchjeong.springiscool.world

import com.hchjeong.springiscool.ai.AiDirector
import com.hchjeong.springiscool.ai.AiDirectorRequest
import com.hchjeong.springiscool.ai.WorldSummary
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
    private val aiDirector: AiDirector,
) {
    fun handle(session: WorldSession, input: String): CommandResult {
        return when (val command = parser.parse(input)) {
            WorldCommand.Empty -> CommandResult.Continue(emptyScene())
            WorldCommand.Look -> CommandResult.Continue(lookScene(session))
            WorldCommand.Answer -> CommandResult.Continue(answerScene(session))
            WorldCommand.Log -> CommandResult.Continue(logScene(session))
            WorldCommand.Status -> CommandResult.Continue(statusScene(session))
            WorldCommand.Help -> CommandResult.Continue(helpScene(session))
            WorldCommand.Quit -> CommandResult.Quit(goodbyeScene())
            is WorldCommand.Ai -> CommandResult.Continue(aiScene(session, command.text))
            is WorldCommand.Assign -> CommandResult.Continue(assignScene(session, command.agentId, command.task))
            is WorldCommand.Unknown -> CommandResult.Continue(unknownScene(session, command.text))
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
        session.record(
            action = WorldAction.Looked,
            observation = "The operator inspected the office.",
        )

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
            session.record(
                action = WorldAction.AnsweredTelephone,
                observation = "The operator listened again to an already open line.",
            )

            return Scene(
                lines = listOf(
                    line("The receiver is already warm in your hand.", delayAfterMillis = 360),
                    blank(160),
                    line("UNKNOWN CALLER:", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                    line("\"I said, you are late.\"", SceneStyle.DIALOGUE, delayAfterMillis = 420),
                ),
            )
        }

        session.record(
            action = WorldAction.AnsweredTelephone,
            observation = "The operator answered the ringing telephone.",
        )

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

    private fun helpScene(session: WorldSession): Scene {
        session.record(
            action = WorldAction.RequestedHelp,
            observation = "The operator requested the command channel.",
        )

        return Scene(
            lines = listOf(
                line("COMMAND CHANNEL", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line("These words are wired:", SceneStyle.MUTED, RevealMode.INSTANT, 180),
                line("  LOOK     inspect the room", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  ANSWER   pick up the telephone", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  STATUS   read the current world state", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  LOG      replay recent events", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  AI ...   ask the director for a scene", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  ASSIGN clerk check line", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  HELP     show this list", SceneStyle.MUTED, RevealMode.INSTANT, 90),
                line("  QUIT     close the line", SceneStyle.MUTED, RevealMode.INSTANT, 160),
                blank(120),
                line("Other words may still reach the line.", SceneStyle.NARRATION, delayAfterMillis = 260),
            ),
        )
    }

    private fun unknownScene(session: WorldSession, input: String): Scene {
        val event = session.record(
            action = WorldAction.UnknownCommand(input),
            observation = "The operator tried an unrecognized command: $input.",
        )

        return Scene(
            lines = listOf(
                line("The system does not recognize `$input`.", SceneStyle.WARNING, delayAfterMillis = 360),
                line("Type HELP if the room feels too quiet.", SceneStyle.MUTED, delayAfterMillis = 240),
                line("EVENT ${event.sequence.toString().padStart(3, '0')} RECORDED.", SceneStyle.SYSTEM, RevealMode.INSTANT, 180),
            ),
        )
    }

    private fun aiScene(session: WorldSession, text: String): Scene {
        if (text.isBlank()) {
            session.record(
                action = WorldAction.RequestedAiDirector,
                observation = "The operator opened the AI director channel without a question.",
            )

            return Scene(
                lines = listOf(
                    line("AI DIRECTOR", SceneStyle.SYSTEM, RevealMode.INSTANT, 200),
                    line("Give the line a sentence after AI.", SceneStyle.MUTED, delayAfterMillis = 260),
                    line("Example: AI what is listening in this room?", SceneStyle.MUTED, RevealMode.INSTANT, 160),
                ),
            )
        }

        val event = session.record(
            action = WorldAction.RequestedAiDirector,
            observation = "The operator asked the AI director: $text",
        )

        val result = aiDirector.direct(
            AiDirectorRequest(
                userText = text,
                worldSummary = WorldSummary.from(session),
                availableTextArt = TextArtLibrary.assetNames(),
            ),
        )

        return result.scene.copy(
            lines = result.scene.lines + line(
                text = "EVENT ${event.sequence.toString().padStart(3, '0')} RECORDED.",
                style = SceneStyle.SYSTEM,
                reveal = RevealMode.INSTANT,
                delayAfterMillis = 180,
            ),
        )
    }

    private fun assignScene(session: WorldSession, agentId: String, task: String): Scene {
        if (agentId.isBlank() || task.isBlank()) {
            session.record(
                action = WorldAction.AssignedTask,
                observation = "The operator opened the assignment channel without a complete delegation.",
                authorityResult = AuthorityResult.DENIED,
            )

            return Scene(
                lines = listOf(
                    line("ASSIGNMENT CHANNEL", SceneStyle.SYSTEM, RevealMode.INSTANT, 180),
                    line("Name an agent and a task.", SceneStyle.MUTED, delayAfterMillis = 240),
                    line("Example: ASSIGN clerk check line", SceneStyle.MUTED, RevealMode.INSTANT, 180),
                ),
            )
        }

        val normalizedAgent = agentId.lowercase()
        val normalizedTask = task.lowercase()
        val allowed = normalizedAgent == "clerk" && normalizedTask == "check line"

        if (!allowed) {
            val event = session.record(
                action = WorldAction.AssignedTask,
                observation = "The operator attempted to delegate `$task` to $normalizedAgent.",
                targetActorId = normalizedAgent,
                authorityResult = AuthorityResult.DENIED,
            )

            return Scene(
                lines = listOf(
                    line("ASSIGNMENT DENIED", SceneStyle.WARNING, RevealMode.INSTANT, 220),
                    line("The office will not grant that authority.", delayAfterMillis = 340),
                    line("$normalizedAgent cannot be assigned `$task` here.", SceneStyle.MUTED, delayAfterMillis = 260),
                    blank(100),
                    line("EVENT ${event.sequence.toString().padStart(3, '0')} RECORDED.", SceneStyle.SYSTEM, RevealMode.INSTANT, 180),
                ),
            )
        }

        val assignment = session.record(
            action = WorldAction.AssignedTask,
            observation = "The operator delegated `check line` to clerk.",
            targetActorId = normalizedAgent,
            authorityResult = AuthorityResult.ALLOWED,
        )
        val agentReport = session.record(
            action = WorldAction.AgentReported,
            observation = "Clerk inspected the carrier signal and reported line noise.",
            actor = Actor("clerk"),
            authorityResult = AuthorityResult.ALLOWED,
        )
        val evidence = session.record(
            action = WorldAction.EvidenceAttached,
            observation = "Evidence `carrier-tone-present` attached to the line check.",
            actor = Actor("clerk"),
            evidenceId = "carrier-tone-present",
        )

        return Scene(
            lines = listOf(
                line("ASSIGNMENT ACCEPTED", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line("You delegate the line check to CLERK.", delayAfterMillis = 360),
                blank(140),
                line("CLERK:", SceneStyle.SYSTEM, RevealMode.INSTANT, 180),
                line("\"I can inspect the signal, not open the door.\"", SceneStyle.DIALOGUE, delayAfterMillis = 420),
                line("\"Carrier tone is present. Line noise is rising.\"", SceneStyle.DIALOGUE, delayAfterMillis = 460),
                blank(120),
                line("EVIDENCE ATTACHED: carrier-tone-present", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line(
                    "EVENTS ${assignment.sequence.toString().padStart(3, '0')}, " +
                        "${agentReport.sequence.toString().padStart(3, '0')}, " +
                        "${evidence.sequence.toString().padStart(3, '0')} RECORDED.",
                    SceneStyle.SYSTEM,
                    RevealMode.INSTANT,
                    180,
                ),
            ),
        )
    }

    private fun logScene(session: WorldSession): Scene {
        val event = session.record(
            action = WorldAction.RequestedHistory,
            observation = "The operator requested the local event log.",
        )

        val historyLines = session.history().takeLast(MAX_LOG_EVENTS).map {
            line(
                text = it.toLogText(),
                style = SceneStyle.MUTED,
                reveal = RevealMode.INSTANT,
                delayAfterMillis = 90,
            )
        }

        return Scene(
            lines = listOf(
                line("LOCAL EVENT LOG", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line("EVENT ${event.sequence.toString().padStart(3, '0')} RECORDED.", SceneStyle.SYSTEM, RevealMode.INSTANT, 160),
                blank(120),
            ) + historyLines,
        )
    }

    private fun statusScene(session: WorldSession): Scene {
        val event = session.record(
            action = WorldAction.CheckedStatus,
            observation = "The operator checked the current world state.",
        )

        val phoneState = if (session.telephoneRinging) "RINGING" else "SILENT"
        val lineState = if (session.lineAnswered) "OPEN" else "UNANSWERED"

        return Scene(
            lines = listOf(
                line("ONTOLOFFICE STATUS", SceneStyle.SYSTEM, RevealMode.INSTANT, 220),
                line("  TELEPHONE   $phoneState", SceneStyle.MUTED, RevealMode.INSTANT, 120),
                line("  LINE        $lineState", SceneStyle.MUTED, RevealMode.INSTANT, 120),
                line("  EVENTS      ${session.history().size}", SceneStyle.MUTED, RevealMode.INSTANT, 160),
                blank(120),
                line("EVENT ${event.sequence.toString().padStart(3, '0')} RECORDED.", SceneStyle.SYSTEM, RevealMode.INSTANT, 180),
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

    companion object {
        private const val MAX_LOG_EVENTS = 8
    }
}

private fun WorldEvent.toLogText(): String {
    val metadata = listOfNotNull(
        targetActorId?.let { "target=$it" },
        authorityResult?.let { "authority=${it.name}" },
        evidenceId?.let { "evidence=$it" },
    )

    val suffix = if (metadata.isEmpty()) "" else " [${metadata.joinToString(" ")}]"
    return "${sequence.toString().padStart(3, '0')} ${actor.id}:${action.verb} - ${observation.text}$suffix"
}

sealed interface CommandResult {
    val scene: Scene

    data class Continue(override val scene: Scene) : CommandResult
    data class Quit(override val scene: Scene) : CommandResult
}
