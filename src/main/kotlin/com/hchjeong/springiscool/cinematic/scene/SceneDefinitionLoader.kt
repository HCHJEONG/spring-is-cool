package com.hchjeong.springiscool.cinematic.scene

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import org.springframework.stereotype.Component

@Component
class SceneDefinitionLoader(
    private val validator: SceneDefinitionValidator = SceneDefinitionValidator(),
) {
    private val objectMapper = jacksonObjectMapper()

    fun loadResource(resourcePath: String): Scene {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Scene resource not found: $resourcePath"
        }

        val definition = stream.use { objectMapper.readValue<SceneDefinition>(it) }
        return toScene(definition)
    }

    fun toScene(definition: SceneDefinition): Scene {
        val validation = validator.validate(definition)
        require(validation.isValid) {
            validation.errors.joinToString(separator = "\n")
        }

        return Scene(
            clearBefore = definition.clearBefore,
            hideCursorDuringPlayback = definition.hideCursorDuringPlayback,
            showPromptAfter = definition.showPromptAfter,
            terminalWidth = definition.terminalWidth,
            lines = definition.lines.map { it.toSceneLine() },
        )
    }

    private fun SceneLineDefinition.toSceneLine(): SceneLine {
        val text = text ?: TextArtLibrary.asset(art.orEmpty())?.text.orEmpty()

        return SceneLine(
            text = text,
            reveal = RevealMode.valueOf(reveal.uppercase()),
            style = SceneStyle.valueOf(style.uppercase()),
            alignment = SceneAlignment.valueOf(alignment.uppercase()),
            delayAfterMillis = delayAfterMillis,
            characterDelayMillis = characterDelayMillis,
        )
    }
}
