package com.hchjeong.springiscool.cinematic.scene

data class SceneDefinition(
    val id: String = "",
    val clearBefore: Boolean = false,
    val hideCursorDuringPlayback: Boolean = false,
    val showPromptAfter: Boolean = true,
    val terminalWidth: Int = 80,
    val lines: List<SceneLineDefinition> = emptyList(),
)

data class SceneLineDefinition(
    val text: String? = null,
    val art: String? = null,
    val reveal: String = "INSTANT",
    val style: String = "NARRATION",
    val alignment: String = "LEFT",
    val delayAfterMillis: Long = 0,
    val characterDelayMillis: Long = 35,
)

data class SceneValidationResult(
    val errors: List<String>,
) {
    val isValid: Boolean = errors.isEmpty()
}
