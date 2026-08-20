package com.hchjeong.springiscool.cinematic.renderer

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SceneTests {
    @Test
    fun `rejects unreasonable line delays`() {
        assertFailsWith<IllegalArgumentException> {
            SceneLine(
                text = "too slow",
                reveal = RevealMode.INSTANT,
                style = SceneStyle.NARRATION,
                delayAfterMillis = 10_001,
            )
        }
    }
}
