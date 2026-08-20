package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneLine
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class ScenePresenterTests {
    private val presenter = ScenePresenter()

    @Test
    fun `projects renderer scene into adapter-neutral scene`() {
        val projected = presenter.present(
            Scene(
                lines = listOf(
                    SceneLine(
                        text = "hello",
                        reveal = RevealMode.INSTANT,
                        style = SceneStyle.SYSTEM,
                        alignment = SceneAlignment.CENTER,
                        delayAfterMillis = 120,
                    ),
                ),
            ),
        )

        assertEquals(80, projected.terminalWidth)
        assertEquals("hello", projected.lines.single().text)
        assertEquals("system", projected.lines.single().style)
        assertEquals("instant", projected.lines.single().reveal)
        assertEquals("center", projected.lines.single().alignment)
    }
}
