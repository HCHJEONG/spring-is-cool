package com.hchjeong.springiscool.world

import com.hchjeong.springiscool.ai.StaticAiDirector
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

        val look = handler.handle(session, "look")
        assertIs<CommandResult.Continue>(look)
        assertTrue(look.scene.lines.any { it.text.contains("rests in its cradle") })
        assertTrue(look.scene.lines.any { it.text.contains("open line") })
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
        assertTrue(session.history().any { it.action == WorldAction.RequestedHistory })
        assertTrue(log.scene.lines.any { it.text.contains("LOCAL EVENT LOG") })
        assertTrue(log.scene.lines.any { it.text.contains("ANSWER") })
    }

    @Test
    fun `status reports current world state`() {
        val session = WorldSession()

        handler.handle(session, "answer")
        val status = handler.handle(session, "status")

        assertIs<CommandResult.Continue>(status)
        assertTrue(status.scene.lines.any { it.text.contains("TELEPHONE   SILENT") })
        assertTrue(status.scene.lines.any { it.text.contains("LINE        OPEN") })
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
    fun `quit returns non prompting quit scene`() {
        val result = handler.handle(WorldSession(), "quit")

        assertIs<CommandResult.Quit>(result)
        assertFalse(result.scene.showPromptAfter)
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
