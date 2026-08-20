package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component

@Component
class IntroSceneProvider {
    fun welcomeScene(): Scene {
        return Scene(
            clearBefore = true,
            hideCursorDuringPlayback = true,
            lines = listOf(
                blank(250),
                SceneLine(
                    text = "The office is empty.",
                    reveal = RevealMode.TYPEWRITER,
                    style = SceneStyle.NARRATION,
                    delayAfterMillis = 900,
                    characterDelayMillis = 45,
                ),
                blank(250),
                TextArtLibrary.telephone(delayAfterMillis = 850),
                blank(250),
                TextArtLibrary.signal("RING...", delayAfterMillis = 1_050),
                blank(350),
                TextArtLibrary.telephone(delayAfterMillis = 700),
                blank(200),
                TextArtLibrary.signal("RING... RING...", delayAfterMillis = 1_200),
                blank(350),
                SceneLine(
                    text = "Someone is calling.",
                    reveal = RevealMode.TYPEWRITER,
                    style = SceneStyle.NARRATION,
                    delayAfterMillis = 900,
                    characterDelayMillis = 70,
                ),
                blank(450),
                SceneLine(
                    text = "LINE OPEN.",
                    reveal = RevealMode.TYPEWRITER,
                    style = SceneStyle.SYSTEM,
                    delayAfterMillis = 650,
                    characterDelayMillis = 30,
                ),
                SceneLine(
                    text = "HELP AVAILABLE.",
                    reveal = RevealMode.INSTANT,
                    style = SceneStyle.MUTED,
                    delayAfterMillis = 450,
                ),
            ),
        )
    }

    private fun blank(delayAfterMillis: Long): SceneLine {
        return SceneLine(
            text = "",
            reveal = RevealMode.INSTANT,
            style = SceneStyle.NARRATION,
            delayAfterMillis = delayAfterMillis,
        )
    }
}
