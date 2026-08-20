package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import org.springframework.stereotype.Component

@Component
class ScenePresenter {
    fun present(scene: Scene): PresentedScene {
        return PresentedScene(
            clearBefore = scene.clearBefore,
            showPromptAfter = scene.showPromptAfter,
            terminalWidth = scene.terminalWidth,
            lines = scene.lines.map { it.present() },
        )
    }

    private fun SceneLine.present(): PresentedSceneLine {
        return PresentedSceneLine(
            text = text,
            style = style.name.lowercase(),
            reveal = reveal.name.lowercase(),
            alignment = alignment.name.lowercase(),
            delayAfterMillis = delayAfterMillis,
            characterDelayMillis = characterDelayMillis,
        )
    }
}

data class PresentedScene(
    val clearBefore: Boolean,
    val showPromptAfter: Boolean,
    val terminalWidth: Int,
    val lines: List<PresentedSceneLine>,
)

data class PresentedSceneLine(
    val text: String,
    val style: String,
    val reveal: String,
    val alignment: String,
    val delayAfterMillis: Long,
    val characterDelayMillis: Long,
)
