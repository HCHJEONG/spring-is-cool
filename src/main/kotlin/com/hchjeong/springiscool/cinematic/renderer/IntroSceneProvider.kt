package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component

@Component
class IntroSceneProvider {
    fun welcomeLines(): List<IntroSceneLine> {
        return listOf(
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 250,
            ),
            IntroSceneLine(
                text = "The office is empty.",
                reveal = RevealMode.TYPEWRITER,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 900,
                characterDelayMillis = 45,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 250,
            ),
            IntroSceneLine(
                text = "                  ☎",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.SIGNAL,
                delayAfterMillis = 850,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 250,
            ),
            IntroSceneLine(
                text = "               RING...",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.SIGNAL,
                delayAfterMillis = 1_050,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 350,
            ),
            IntroSceneLine(
                text = "                  ☎",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.SIGNAL,
                delayAfterMillis = 700,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 200,
            ),
            IntroSceneLine(
                text = "            RING... RING...",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.SIGNAL,
                delayAfterMillis = 1_200,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 350,
            ),
            IntroSceneLine(
                text = "Someone is calling.",
                reveal = RevealMode.TYPEWRITER,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 900,
                characterDelayMillis = 70,
            ),
            IntroSceneLine(
                text = "",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 450,
            ),
            IntroSceneLine(
                text = "LINE OPEN.",
                reveal = RevealMode.TYPEWRITER,
                style = SceneStyle.SYSTEM,
                delayAfterMillis = 650,
                characterDelayMillis = 30,
            ),
        )
    }
}
