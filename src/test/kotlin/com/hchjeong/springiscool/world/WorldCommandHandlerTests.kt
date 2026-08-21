package com.hchjeong.springiscool.world

import com.hchjeong.springiscool.ai.AiDirector
import com.hchjeong.springiscool.ai.AiDirectorRequest
import com.hchjeong.springiscool.ai.AiDirectorResult
import com.hchjeong.springiscool.ai.StaticAiDirector
import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorldCommandHandlerTests {
    private val handler = WorldCommandHandler(
        CommandParser(),
        StaticAiDirector(SceneDefinitionLoader()),
    )

    @Test
    fun `answer changes telephone state and later look reflects it`() {
        val session = WorldSession()

        val answer = handler.handle(session, "answer")
        assertIs<CommandResult.Continue>(answer)
        assertFalse(session.telephoneRinging)
        assertTrue(session.lineAnswered)
        assertTrue(session.lineOffline)
        assertTrue(answer.scene.lines.any { it.text.contains("Type LOOK") })
        assertTrue(answer.scene.lines.any { it.text.contains("line goes offline") })

        val look = handler.handle(session, "look")
        assertIs<CommandResult.Continue>(look)
        assertTrue(look.scene.lines.any { it.text.contains("line is offline") })
        assertTrue(look.scene.lines.any { it.text.contains("instruction remains") })
        assertTrue(look.scene.lines.any { it.text.contains("Instruction status: COMPLETED") })
    }

    @Test
    fun `commands are recorded as world events`() {
        val session = WorldSession()

        handler.handle(session, "look")
        handler.handle(session, "answer")
        val log = handler.handle(session, "log")

        assertIs<CommandResult.Continue>(log)
        assertTrue(session.history().any { it.action == WorldAction.Looked })
        assertTrue(session.history().any { it.action == WorldAction.AnsweredTelephone })
        assertTrue(session.history().any { it.action == WorldAction.LineWentOffline })
        assertTrue(session.history().any { it.action == WorldAction.RequestedHistory })
        assertTrue(log.scene.lines.any { it.text.contains("LOCAL EVENT LOG") })
        assertTrue(log.scene.lines.any { it.text.contains("ANSWER") })
        assertTrue(log.scene.lines.any { it.text.contains("OFFLINE") })
    }

    @Test
    fun `status reports current world state`() {
        val session = WorldSession()

        handler.handle(session, "answer")
        val status = handler.handle(session, "status")

        assertIs<CommandResult.Continue>(status)
        assertTrue(status.scene.lines.any { it.text.contains("TELEPHONE   SILENT") })
        assertTrue(status.scene.lines.any { it.text.contains("LINE        OFFLINE") })
        assertTrue(status.scene.lines.any { it.text.contains("INSTR STATE NOT COMPLETED") })
    }

    @Test
    fun `status reports completed instruction after look follows answer`() {
        val session = WorldSession()

        handler.handle(session, "answer")
        handler.handle(session, "look")
        val status = handler.handle(session, "status")

        assertIs<CommandResult.Continue>(status)
        assertTrue(status.scene.lines.any { it.text.contains("INSTR STATE COMPLETED") })
    }

    @Test
    fun `look and status show connected users and AI clerk presence`() {
        val presenceRegistry = PresenceRegistry()
        val firstUser = presenceRegistry.enterSsh()
        val secondUser = presenceRegistry.enterSsh()
        val handler = WorldCommandHandler(
            CommandParser(),
            StaticAiDirector(SceneDefinitionLoader()),
            presenceRegistry,
        )

        try {
            val look = handler.handle(WorldSession(), "look")
            val status = handler.handle(WorldSession(), "status")

            assertIs<CommandResult.Continue>(look)
            assertIs<CommandResult.Continue>(status)
            assertTrue(look.scene.lines.any { it.text.contains("Users in office: 2") })
            assertTrue(look.scene.lines.any { it.text.contains("AI clerk terminal: STANDBY") })
            assertTrue(look.scene.lines.any { it.text.contains("SSH users may: inspect office") })
            assertTrue(look.scene.lines.any { it.text.contains("AI clerk may: check line state") })
            assertTrue(look.scene.lines.any { it.text.contains("file signal evidence") })
            assertTrue(status.scene.lines.any { it.text.contains("USERS       2") })
            assertTrue(status.scene.lines.any { it.text.contains("AGENT       AI clerk: STANDBY") })
            assertTrue(status.scene.lines.any { it.text.contains("AUTHORITY   SSH users: inspect office") })
            assertTrue(status.scene.lines.any { it.text.contains("AUTHORITY   AI clerk: check line state") })
            assertTrue(status.scene.lines.any { it.text.contains("file signal evidence") })
        } finally {
            firstUser.close()
            secondUser.close()
        }
    }

    @Test
    fun `unknown commands are recorded`() {
        val session = WorldSession()

        val result = handler.handle(session, "open drawer")

        assertIs<CommandResult.Continue>(result)
        assertTrue(session.history().any { it.action is WorldAction.UnknownCommand })
        assertTrue(result.scene.lines.any { it.text.contains("EVENT") })
    }

    @Test
    fun `ai command records event and returns validated scene`() {
        val session = WorldSession()

        val result = handler.handle(session, "ai what is listening?")

        assertIs<CommandResult.Continue>(result)
        assertTrue(session.history().any { it.action == WorldAction.RequestedAiDirector })
        assertTrue(result.scene.lines.any { it.text.contains("AI DIRECTOR") })
        assertTrue(result.scene.lines.any { it.text.contains("EVENT") })
    }

    @Test
    fun `ai fallback hides technical reason until log is requested`() {
        val handler = WorldCommandHandler(
            CommandParser(),
            FailingAiDirector("Line 1 field `text` must be a string."),
        )
        val session = WorldSession()

        val ai = handler.handle(session, "ai check office state")
        val log = handler.handle(session, "log")

        assertIs<CommandResult.Continue>(ai)
        assertIs<CommandResult.Continue>(log)
        assertFalse(ai.scene.lines.any { it.text.contains("Line 1 field") })
        assertTrue(log.scene.lines.any { it.text.contains("ai-director:AGENT") && it.text.contains("Line 1 field") })
    }

    @Test
    fun `assign delegates allowed task to fake agent and attaches evidence`() {
        val session = WorldSession()

        val result = handler.handle(session, "assign AI clerk check line")

        assertIs<CommandResult.Continue>(result)
        assertTrue(session.history().any {
            it.action == WorldAction.AssignedTask &&
                it.targetActorId == "ai-clerk" &&
                it.authorityResult == AuthorityResult.ALLOWED
        })
        assertTrue(session.history().any { it.actor.id == "ai-clerk" && it.action == WorldAction.AgentReported })
        assertTrue(session.history().any {
            it.action == WorldAction.EvidenceAttached &&
                it.evidenceId == "carrier-tone-present"
        })
        assertTrue(result.scene.lines.any { it.text.contains("EVIDENCE FILED") })
    }

    @Test
    fun `assign records denied authority result for unsupported delegation`() {
        val session = WorldSession()

        val result = handler.handle(session, "assign clerk open door")

        assertIs<CommandResult.Continue>(result)
        assertTrue(session.history().any {
            it.action == WorldAction.AssignedTask &&
                it.authorityResult == AuthorityResult.DENIED
        })
        assertTrue(result.scene.lines.any { it.text.contains("ASSIGNMENT DENIED") })
    }

    @Test
    fun `log shows actor authority target and evidence metadata`() {
        val session = WorldSession()

        handler.handle(session, "assign AI clerk check line")
        val log = handler.handle(session, "log")

        assertIs<CommandResult.Continue>(log)
        assertTrue(log.scene.lines.any { it.text.contains("operator:ASSIGN") && it.text.contains("target=ai-clerk") })
        assertTrue(log.scene.lines.any { it.text.contains("ai-clerk:AGENT") && it.text.contains("authority=ALLOWED") })
        assertTrue(log.scene.lines.any { it.text.contains("evidence=carrier-tone-present") })
    }

    @Test
    fun `quit returns operations room terminal scene`() {
        val result = handler.handle(WorldSession(), "quit")

        assertIs<CommandResult.Quit>(result)
        assertTrue(result.scene.showPromptAfter)
        assertTrue(result.scene.lines.any { it.text.contains("MAINFRAME OPERATIONS ROOM") })
        assertTrue(result.scene.lines.any { it.text.contains("LOCAL TERMINAL") })
    }

    @Test
    fun `help is the only command advertised by the intro`() {
        val intro = com.hchjeong.springiscool.cinematic.renderer.IntroSceneProvider().welcomeScene()
        val introText = intro.lines.joinToString("\n") { it.text }

        assertTrue(introText.contains("ONTOLOFFICE"))
        assertTrue(introText.contains("HELP AVAILABLE."))
        assertFalse(introText.contains("LOOK"))
        assertFalse(introText.contains("ANSWER"))
        assertFalse(introText.contains("QUIT"))
    }
}

private class FailingAiDirector(
    private val reason: String,
) : AiDirector {
    override fun direct(request: AiDirectorRequest): AiDirectorResult {
        return AiDirectorResult.Fallback(
            scene = Scene(
                lines = listOf(
                    SceneLine(
                        text = "AI provider signal collapsed. Static control retained.",
                        reveal = RevealMode.INSTANT,
                        style = SceneStyle.WARNING,
                        delayAfterMillis = 0,
                    ),
                    SceneLine(
                        text = "The failure was recorded in the local event log.",
                        reveal = RevealMode.INSTANT,
                        style = SceneStyle.MUTED,
                        delayAfterMillis = 0,
                    ),
                ),
            ),
            reason = reason,
        )
    }
}
