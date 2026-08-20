package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.ai.StaticAiDirector
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import com.hchjeong.springiscool.world.CommandParser
import com.hchjeong.springiscool.world.WorldCommandHandler
import com.hchjeong.springiscool.world.WorldSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldInteractionServiceTests {
    private val service = WorldInteractionService(
        WorldCommandHandler(
            CommandParser(),
            StaticAiDirector(SceneDefinitionLoader()),
        ),
        ScenePresenter(),
    )

    @Test
    fun `returns adapter-neutral scene and continue outcome`() {
        val result = service.submit(WorldSession(), "look")

        assertEquals(InteractionOutcome.CONTINUE, result.outcome)
        assertTrue(result.presentedScene.lines.any { it.text.contains("office waits") })
        assertEquals(result.rendererScene.lines.size, result.presentedScene.lines.size)
    }

    @Test
    fun `returns quit outcome for closing command`() {
        val result = service.submit(WorldSession(), "quit")

        assertEquals(InteractionOutcome.QUIT, result.outcome)
        assertEquals(true, result.presentedScene.showPromptAfter)
        assertTrue(result.presentedScene.lines.any { it.text.contains("MAINFRAME OPERATIONS ROOM") })
    }
}
