package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import com.hchjeong.springiscool.cinematic.scene.SceneDefinition
import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import com.hchjeong.springiscool.cinematic.scene.SceneLineDefinition
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "spring-is-cool.ai",
    name = ["provider"],
    havingValue = "static",
    matchIfMissing = true,
)
class StaticAiDirector(
    private val sceneDefinitionLoader: SceneDefinitionLoader,
) : AiDirector {
    override fun direct(request: AiDirectorRequest): AiDirectorResult {
        val definition = buildDefinition(request)

        return runCatching {
            AiDirectorResult.Generated(
                scene = sceneDefinitionLoader.toScene(definition),
                definition = definition,
                reviewNotes = reviewNotes(request),
            )
        }.getOrElse {
            AiDirectorResult.Fallback(
                scene = fallbackScene("static AI clerk output failed validation"),
                reason = it.message.orEmpty(),
            )
        }
    }

    private fun buildDefinition(request: AiDirectorRequest): SceneDefinition {
        val lineState = when {
            request.worldSummary.lineOffline -> "The line is offline, but the instruction remains."
            request.worldSummary.lineAnswered -> "The open line listens before it answers."
            else -> "The unanswered line keeps ringing behind the wall."
        }

        val echo = request.userText
            .replace(Regex("\\s+"), " ")
            .take(MAX_ECHO_LENGTH)

        return SceneDefinition(
            id = "ai-static-response",
            terminalWidth = 80,
            lines = listOf(
                SceneLineDefinition(
                    text = "AI CLERK",
                    style = "SYSTEM",
                    reveal = "INSTANT",
                    delayAfterMillis = 180,
                ),
                SceneLineDefinition(
                    art = "signal-marker",
                    style = "SIGNAL",
                    alignment = "CENTER",
                    delayAfterMillis = 260,
                ),
                SceneLineDefinition(
                    text = "The office studies your words:",
                    style = "MUTED",
                    delayAfterMillis = 220,
                ),
                SceneLineDefinition(
                    text = "\"$echo\"",
                    style = "DIALOGUE",
                    delayAfterMillis = 420,
                ),
                SceneLineDefinition(
                    text = lineState,
                    style = "NARRATION",
                    delayAfterMillis = 360,
                ),
                SceneLineDefinition(
                    text = "No raw terminal control was accepted.",
                    style = "MUTED",
                    reveal = "INSTANT",
                    delayAfterMillis = 180,
                ),
            ),
        )
    }

    private fun reviewNotes(request: AiDirectorRequest): List<String> {
        return listOf(
            "Used deterministic static AI clerk response while provider is not enabled.",
            "Referenced ${TextArtLibrary.assetNames().intersect(request.availableTextArt.toSet()).sorted()} from the text-art library.",
            "Output was validated as SceneDefinition before rendering.",
        )
    }

    private fun fallbackScene(reason: String): Scene {
        return Scene(
            lines = listOf(
                SceneLine(
                    text = "The line refuses the generated signal.",
                    reveal = RevealMode.TYPEWRITER,
                    style = SceneStyle.WARNING,
                    delayAfterMillis = 260,
                ),
                SceneLine(
                    text = reason,
                    reveal = RevealMode.INSTANT,
                    style = SceneStyle.MUTED,
                    alignment = SceneAlignment.LEFT,
                    delayAfterMillis = 180,
                ),
            ),
        )
    }

    companion object {
        private const val MAX_ECHO_LENGTH = 80
    }
}
