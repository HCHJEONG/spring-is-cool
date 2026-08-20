package com.hchjeong.springiscool.world

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WorldCommandHandlerTests {
    private val handler = WorldCommandHandler(CommandParser())

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
    fun `quit returns non prompting quit scene`() {
        val result = handler.handle(WorldSession(), "quit")

        assertIs<CommandResult.Quit>(result)
        assertFalse(result.scene.showPromptAfter)
    }

    @Test
    fun `help is the only command advertised by the intro`() {
        val intro = com.hchjeong.springiscool.cinematic.renderer.IntroSceneProvider().welcomeScene()
        val introText = intro.lines.joinToString("\n") { it.text }

        assertTrue(introText.contains("HELP AVAILABLE."))
        assertFalse(introText.contains("LOOK"))
        assertFalse(introText.contains("ANSWER"))
        assertFalse(introText.contains("QUIT"))
    }
}
