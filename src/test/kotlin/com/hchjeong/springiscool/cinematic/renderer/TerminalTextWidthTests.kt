package com.hchjeong.springiscool.cinematic.renderer

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminalTextWidthTests {
    @Test
    fun `counts CJK and emoji as wide cells`() {
        assertEquals(1, TerminalTextWidth.visibleWidth("A"))
        assertEquals(2, TerminalTextWidth.visibleWidth("한"))
        assertEquals(2, TerminalTextWidth.visibleWidth("\uD83D\uDCDE"))
        assertEquals(5, TerminalTextWidth.visibleWidth("A한\uD83D\uDCDE"))
    }

    @Test
    fun `does not count combining marks as visible cells`() {
        assertEquals(1, TerminalTextWidth.visibleWidth("e\u0301"))
    }
}
