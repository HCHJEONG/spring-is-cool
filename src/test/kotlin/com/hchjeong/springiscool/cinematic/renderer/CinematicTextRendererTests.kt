package com.hchjeong.springiscool.cinematic.renderer

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContains

class CinematicTextRendererTests {
    private val renderer = CinematicTextRenderer(IntroSceneProvider())

    @Test
    fun `renders scene lines and prompt`() {
        val output = ByteArrayOutputStream()
        val scene = Scene(
            lines = listOf(
                SceneLine(
                    text = "LOOK",
                    reveal = RevealMode.INSTANT,
                    style = SceneStyle.SYSTEM,
                    delayAfterMillis = 0,
                ),
            ),
        )

        renderer.render(output, scene, timingProfile = TimingProfile.INSTANT)

        val rendered = output.toString(Charsets.UTF_8)
        assertContains(rendered, "LOOK")
        assertContains(rendered, "> _")
    }

    @Test
    fun `renders same scene with monochrome theme`() {
        val output = ByteArrayOutputStream()
        val scene = Scene(
            lines = listOf(
                SceneLine(
                    text = "WARNING",
                    reveal = RevealMode.INSTANT,
                    style = SceneStyle.WARNING,
                    delayAfterMillis = 0,
                ),
            ),
            showPromptAfter = false,
        )

        renderer.render(
            output = output,
            scene = scene,
            theme = MonochromeTerminalTheme,
            timingProfile = TimingProfile.INSTANT,
        )

        val rendered = output.toString(Charsets.UTF_8)
        assertContains(rendered, "\u001B[1mWARNING\u001B[0m")
    }

    @Test
    fun `applies scene line alignment before styling`() {
        val output = ByteArrayOutputStream()
        val scene = Scene(
            terminalWidth = 20,
            showPromptAfter = false,
            lines = listOf(
                SceneLine(
                    text = "AB",
                    reveal = RevealMode.INSTANT,
                    style = SceneStyle.SYSTEM,
                    delayAfterMillis = 0,
                    alignment = SceneAlignment.CENTER,
                ),
            ),
        )

        renderer.render(output, scene, timingProfile = TimingProfile.INSTANT)

        val rendered = output.toString(Charsets.UTF_8)
        assertContains(rendered, "         \u001B[38;5;81mAB\u001B[0m")
    }
}
