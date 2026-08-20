package com.hchjeong.springiscool.cinematic.renderer

data class Scene(
    val lines: List<SceneLine>,
    val clearBefore: Boolean = false,
    val hideCursorDuringPlayback: Boolean = false,
    val showPromptAfter: Boolean = true,
    val terminalWidth: Int = DEFAULT_TERMINAL_WIDTH,
) {
    init {
        require(lines.size <= MAX_LINES) { "Scene cannot contain more than $MAX_LINES lines." }
        require(terminalWidth in MIN_TERMINAL_WIDTH..MAX_TERMINAL_WIDTH) {
            "Terminal width must be between $MIN_TERMINAL_WIDTH and $MAX_TERMINAL_WIDTH cells."
        }
    }

    companion object {
        const val DEFAULT_TERMINAL_WIDTH = 80
        private const val MIN_TERMINAL_WIDTH = 20
        private const val MAX_TERMINAL_WIDTH = 300
        private const val MAX_LINES = 200
    }
}

data class SceneLine(
    val text: String,
    val reveal: RevealMode,
    val style: SceneStyle,
    val delayAfterMillis: Long,
    val characterDelayMillis: Long = 35,
    val alignment: SceneAlignment = SceneAlignment.LEFT,
) {
    init {
        require(delayAfterMillis in 0..MAX_DELAY_MILLIS) {
            "Line delay must be between 0 and $MAX_DELAY_MILLIS milliseconds."
        }
        require(characterDelayMillis in 0..MAX_CHARACTER_DELAY_MILLIS) {
            "Character delay must be between 0 and $MAX_CHARACTER_DELAY_MILLIS milliseconds."
        }
    }

    companion object {
        private const val MAX_DELAY_MILLIS = 10_000L
        private const val MAX_CHARACTER_DELAY_MILLIS = 500L
    }
}

enum class SceneAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

enum class RevealMode {
    INSTANT,
    TYPEWRITER,
}

enum class SceneStyle {
    NARRATION,
    SIGNAL,
    PROMPT,
    SYSTEM,
    DIALOGUE,
    WARNING,
    MUTED,
}
