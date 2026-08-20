package com.hchjeong.springiscool.cinematic.scene

import com.hchjeong.springiscool.cinematic.renderer.RevealMode
import com.hchjeong.springiscool.cinematic.renderer.SceneAlignment
import com.hchjeong.springiscool.cinematic.renderer.SceneStyle
import com.hchjeong.springiscool.cinematic.renderer.TerminalTextWidth
import com.hchjeong.springiscool.cinematic.renderer.TextArtLibrary
import org.springframework.stereotype.Component

@Component
class SceneDefinitionValidator {
    fun validate(definition: SceneDefinition): SceneValidationResult {
        val errors = mutableListOf<String>()

        if (definition.id.isBlank()) {
            errors += "Scene id is required."
        }
        if (definition.terminalWidth !in 20..300) {
            errors += "Terminal width must be between 20 and 300 cells."
        }
        if (definition.lines.size > 200) {
            errors += "Scene cannot contain more than 200 lines."
        }

        var totalDurationMillis = 0L
        definition.lines.forEachIndexed { index, line ->
            validateLine(index, line, definition.terminalWidth, errors)
            totalDurationMillis += line.delayAfterMillis.coerceAtLeast(0)
            totalDurationMillis += (line.text ?: "").length * line.characterDelayMillis.coerceAtLeast(0)
        }

        if (totalDurationMillis > 120_000) {
            errors += "Scene duration estimate exceeds 120000 milliseconds."
        }

        return SceneValidationResult(errors)
    }

    private fun validateLine(
        index: Int,
        line: SceneLineDefinition,
        terminalWidth: Int,
        errors: MutableList<String>,
    ) {
        val label = "Line ${index + 1}"
        val hasText = line.text != null
        val hasArt = line.art != null

        if (hasText == hasArt) {
            errors += "$label must define exactly one of text or art."
        }
        if (line.text?.contains("\u001B") == true) {
            errors += "$label contains raw ANSI escape characters."
        }
        if (hasArt && TextArtLibrary.asset(line.art.orEmpty()) == null) {
            errors += "$label references unknown text art asset `${line.art}`."
        }
        if (enumValueOrNull<RevealMode>(line.reveal) == null) {
            errors += "$label has unknown reveal mode `${line.reveal}`."
        }
        if (enumValueOrNull<SceneStyle>(line.style) == null) {
            errors += "$label has unknown scene style `${line.style}`."
        }
        if (enumValueOrNull<SceneAlignment>(line.alignment) == null) {
            errors += "$label has unknown alignment `${line.alignment}`."
        }
        if (line.delayAfterMillis !in 0..10_000) {
            errors += "$label delay must be between 0 and 10000 milliseconds."
        }
        if (line.characterDelayMillis !in 0..500) {
            errors += "$label character delay must be between 0 and 500 milliseconds."
        }

        val visibleText = line.text ?: TextArtLibrary.asset(line.art.orEmpty())?.text.orEmpty()
        if (TerminalTextWidth.visibleWidth(visibleText) > terminalWidth) {
            errors += "$label is wider than the terminal."
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return enumValues<T>().firstOrNull { it.name == value.uppercase() }
    }
}
