package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.ai.StaticAiDirector
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import com.hchjeong.springiscool.persistence.DisabledWorldEventStore
import com.hchjeong.springiscool.persistence.PersistenceProperties
import com.hchjeong.springiscool.persistence.WorldSessionFactory
import com.hchjeong.springiscool.world.CommandParser
import com.hchjeong.springiscool.world.WorldCommandHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorldCommandControllerTests {
    private val controller = WorldCommandController(
        WorldSessionFactory(
            DisabledWorldEventStore(),
            PersistenceProperties(sessionId = "controller-test-office"),
        ),
        WorldInteractionService(
            WorldCommandHandler(
                CommandParser(),
                StaticAiDirector(SceneDefinitionLoader()),
            ),
            ScenePresenter(),
        ),
    )

    @Test
    fun `submits command and returns presented scene response`() {
        val response = controller.submit(WorldCommandRequest(command = "look"))
        val body = assertNotNull(response.body)

        assertEquals("controller-test-office", body.sessionId)
        assertEquals("continue", body.outcome)
        assertTrue(body.scene.lines.any { it.text == "The office waits in green phosphor silence." })
    }
}
