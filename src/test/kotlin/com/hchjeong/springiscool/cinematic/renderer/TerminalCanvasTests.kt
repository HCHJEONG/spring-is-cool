package com.hchjeong.springiscool.cinematic.renderer

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalCanvasTests {
    @Test
    fun `centers text by visible cell width`() {
        val canvas = TerminalCanvas(width = 10)

        assertEquals("    ", canvas.padFor("AB", SceneAlignment.CENTER))
        assertEquals("   ", canvas.padFor("한A", SceneAlignment.CENTER))
    }

    @Test
    fun `right aligns text by visible cell width`() {
        val canvas = TerminalCanvas(width = 10)

        assertEquals("        ", canvas.padFor("AB", SceneAlignment.RIGHT))
        assertEquals("       ", canvas.padFor("한A", SceneAlignment.RIGHT))
    }
}
