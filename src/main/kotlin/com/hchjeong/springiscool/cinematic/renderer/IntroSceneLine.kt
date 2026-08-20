package com.hchjeong.springiscool.cinematic.renderer

data class IntroSceneLine(
    val text: String,
    val reveal: RevealMode,
    val style: SceneStyle,
    val delayAfterMillis: Long,
    val characterDelayMillis: Long = 35,
)

enum class RevealMode {
    INSTANT,
    TYPEWRITER,
}

enum class SceneStyle {
    NARRATION,
    SIGNAL,
    PROMPT,
    SYSTEM,
}
