package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import com.hchjeong.springiscool.world.WorldSession
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeminiAiDirectorTests {
    @Test
    fun `uses provider text as validated scene definition`() {
        val director = GeminiAiDirector(
            properties = AiDirectorProperties(
                enabled = true,
                provider = "gemini",
                project = "demo-project",
                location = "global",
                modelId = "gemini-2.5-flash-lite",
            ),
            client = FakeGeminiTextClient(
                """
                {
                  "id": "gemini-test",
                  "terminalWidth": 80,
                  "lines": [
                    {
                      "text": "GEMINI DIRECTOR",
                      "style": "SYSTEM",
                      "reveal": "INSTANT",
                      "delayAfterMillis": 10
                    }
                  ]
                }
                """.trimIndent(),
            ),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Generated>(result)
        assertTrue(result.scene.lines.any { it.text == "GEMINI DIRECTOR" })
        assertTrue(result.reviewNotes.any { it.contains("Gemini") })
    }

    @Test
    fun `falls back when provider output is invalid`() {
        val director = GeminiAiDirector(
            properties = AiDirectorProperties(
                enabled = true,
                provider = "gemini",
                project = "demo-project",
                location = "global",
                modelId = "gemini-2.5-flash-lite",
            ),
            client = FakeGeminiTextClient("""{"id":"","lines":[{"text":"bad","art":"telephone"}]}"""),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Fallback>(result)
        assertTrue(result.scene.lines.any { it.text.contains("Gemini signal collapsed") })
    }

    @Test
    fun `ignores unknown provider fields before validation`() {
        val director = GeminiAiDirector(
            properties = AiDirectorProperties(
                enabled = true,
                provider = "gemini",
                project = "demo-project",
                location = "global",
                modelId = "gemini-2.5-flash-lite",
            ),
            client = FakeGeminiTextClient(
                """
                {
                  "id": "gemini-extra-field",
                  "terminalWidth": 80,
                  "lines": [
                    {
                      "text": "Extra fields are ignored.",
                      "asset": "telephone",
                      "style": "SYSTEM",
                      "reveal": "INSTANT",
                      "delayAfterMillis": 10
                    }
                  ]
                }
                """.trimIndent(),
            ),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Generated>(result)
        assertTrue(result.scene.lines.any { it.text == "Extra fields are ignored." })
    }

    private fun request(): AiDirectorRequest {
        return AiDirectorRequest(
            userText = "what is listening?",
            worldSummary = WorldSummary.from(WorldSession()),
            availableTextArt = TextArtLibrary.assetNames(),
        )
    }
}

private class FakeGeminiTextClient(
    private val text: String,
) : GeminiTextClient {
    override fun generate(prompt: String): String {
        return text
    }
}
