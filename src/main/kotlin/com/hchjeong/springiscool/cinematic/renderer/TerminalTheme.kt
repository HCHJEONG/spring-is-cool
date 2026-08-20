package com.hchjeong.springiscool.cinematic.renderer

interface TerminalTheme {
    fun ansiFor(style: SceneStyle): String
}

object GreenCrtTerminalTheme : TerminalTheme {
    override fun ansiFor(style: SceneStyle): String {
        return when (style) {
            SceneStyle.NARRATION -> "\u001B[38;5;114m"
            SceneStyle.SIGNAL -> "\u001B[38;5;191;1m"
            SceneStyle.PROMPT -> "\u001B[38;5;120;1m"
            SceneStyle.SYSTEM -> "\u001B[38;5;81m"
            SceneStyle.DIALOGUE -> "\u001B[38;5;229m"
            SceneStyle.WARNING -> "\u001B[38;5;214;1m"
            SceneStyle.MUTED -> "\u001B[38;5;65m"
        }
    }
}

object MonochromeTerminalTheme : TerminalTheme {
    override fun ansiFor(style: SceneStyle): String {
        return when (style) {
            SceneStyle.SIGNAL,
            SceneStyle.PROMPT,
            SceneStyle.SYSTEM,
            SceneStyle.WARNING,
            -> "\u001B[1m"
            SceneStyle.NARRATION,
            SceneStyle.DIALOGUE,
            SceneStyle.MUTED,
            -> ""
        }
    }
}
