package com.hchjeong.springiscool.cinematic.scene

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneDefinitionValidatorTests {
    private val validator = SceneDefinitionValidator()

    @Test
    fun `accepts valid text art scene definitions`() {
        val result = validator.validate(
            SceneDefinition(
                id = "valid",
                lines = listOf(
                    SceneLineDefinition(
                        art = "telephone",
                        style = "SIGNAL",
                        alignment = "CENTER",
                        delayAfterMillis = 100,
                    ),
                ),
            ),
        )

        assertTrue(result.isValid)
    }

    @Test
    fun `rejects raw ansi and unknown art`() {
        val result = validator.validate(
            SceneDefinition(
                id = "unsafe",
                lines = listOf(
                    SceneLineDefinition(text = "\u001B[31mred", style = "NARRATION"),
                    SceneLineDefinition(art = "not-real", style = "SIGNAL"),
                ),
            ),
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("raw ANSI") })
        assertTrue(result.errors.any { it.contains("unknown text art asset") })
    }

    @Test
    fun `rejects unknown style and reveal mode`() {
        val result = validator.validate(
            SceneDefinition(
                id = "unknown",
                lines = listOf(
                    SceneLineDefinition(
                        text = "hello",
                        reveal = "FADE",
                        style = "LOUD",
                    ),
                ),
            ),
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("unknown reveal mode") })
        assertTrue(result.errors.any { it.contains("unknown scene style") })
    }
}
