package com.hchjeong.springiscool.cinematic.scene

import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SceneDefinitionLoaderTests {
    private val loader = SceneDefinitionLoader()

    @Test
    fun `loads Ontoloffice intro from resource scene definition`() {
        val scene = loader.loadResource("scenes/ontoloffice-intro.json")

        assertTrue(scene.clearBefore)
        assertTrue(scene.hideCursorDuringPlayback)
        assertTrue(scene.lines.any { it.text == "ONTOLOFFICE" })
        assertTrue(scene.lines.any { it.text == "\u260E" && it.alignment == SceneAlignment.CENTER })
    }

    @Test
    fun `converts scene definition into renderer scene`() {
        val scene = loader.toScene(
            SceneDefinition(
                id = "inline",
                showPromptAfter = false,
                lines = listOf(
                    SceneLineDefinition(
                        text = "SYSTEM READY",
                        style = "SYSTEM",
                        alignment = "CENTER",
                        delayAfterMillis = 0,
                    ),
                ),
            ),
        )

        assertEquals(false, scene.showPromptAfter)
        assertEquals("SYSTEM READY", scene.lines.single().text)
        assertEquals(SceneStyle.SYSTEM, scene.lines.single().style)
        assertEquals(SceneAlignment.CENTER, scene.lines.single().alignment)
    }

    @Test
    fun `refuses invalid scene definitions`() {
        assertFailsWith<IllegalArgumentException> {
            loader.toScene(
                SceneDefinition(
                    id = "bad",
                    lines = listOf(
                        SceneLineDefinition(text = "\u001B[2J", style = "SYSTEM"),
                    ),
                ),
            )
        }
    }
}
