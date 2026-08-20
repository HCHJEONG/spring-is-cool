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
            callLimiter = FakeAiProviderCallLimiter(),
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
            callLimiter = FakeAiProviderCallLimiter(),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Fallback>(result)
        assertTrue(result.scene.lines.any { it.text.contains("AI provider signal collapsed") })
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
            callLimiter = FakeAiProviderCallLimiter(),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Generated>(result)
        assertTrue(result.scene.lines.any { it.text == "Extra fields are ignored." })
    }

    @Test
    fun `falls back without calling provider when monthly limit is exhausted`() {
        val client = FakeGeminiTextClient(
            """
            {
              "id": "should-not-run",
              "terminalWidth": 80,
              "lines": [
                {
                  "text": "SHOULD NOT RUN",
                  "style": "SYSTEM",
                  "reveal": "INSTANT",
                  "delayAfterMillis": 10
                }
              ]
            }
            """.trimIndent(),
        )
        val director = GeminiAiDirector(
            properties = AiDirectorProperties(
                enabled = true,
                provider = "gemini",
                project = "demo-project",
                location = "global",
                modelId = "gemini-2.5-flash-lite",
            ),
            client = client,
            callLimiter = FakeAiProviderCallLimiter(allowed = false),
            sceneDefinitionLoader = SceneDefinitionLoader(),
        )

        val result = director.direct(request())

        assertIs<AiDirectorResult.Fallback>(result)
        assertTrue(result.scene.lines.any { it.text.contains("monthly call limit") })
        assertTrue(client.calls == 0)
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
    var calls: Int = 0
        private set

    override fun generate(prompt: String): String {
        calls += 1
        return text
    }
}

private class FakeAiProviderCallLimiter(
    private val allowed: Boolean = true,
) : AiProviderCallLimiter {
    override fun tryAcquire(provider: String, modelId: String): AiProviderCallLimitResult {
        return AiProviderCallLimitResult(
            allowed = allowed,
            provider = provider,
            modelId = modelId,
            month = java.time.YearMonth.of(2026, 8),
            used = if (allowed) 1 else 250,
            limit = 250,
        )
    }
}
