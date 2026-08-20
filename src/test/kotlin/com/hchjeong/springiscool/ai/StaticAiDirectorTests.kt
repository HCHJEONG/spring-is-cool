package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import com.hchjeong.springiscool.world.WorldSession
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StaticAiDirectorTests {
    private val director = StaticAiDirector(SceneDefinitionLoader())

    @Test
    fun `returns generated scene through validation boundary`() {
        val result = director.direct(
            AiDirectorRequest(
                userText = "what is listening in the room?",
                worldSummary = WorldSummary.from(WorldSession()),
                availableTextArt = TextArtLibrary.assetNames(),
            ),
        )

        assertIs<AiDirectorResult.Generated>(result)
        assertTrue(result.scene.lines.any { it.text.contains("AI DIRECTOR") })
        assertTrue(result.reviewNotes.any { it.contains("validated") })
    }
}
