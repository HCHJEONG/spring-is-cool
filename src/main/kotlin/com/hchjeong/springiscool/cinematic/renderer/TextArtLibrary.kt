package com.hchjeong.springiscool.cinematic.renderer

object TextArtLibrary {
    fun telephone(style: SceneStyle = SceneStyle.SIGNAL, delayAfterMillis: Long = 700): SceneLine {
        return SceneLine(
            text = "\u260E",
            reveal = RevealMode.INSTANT,
            style = style,
            delayAfterMillis = delayAfterMillis,
            alignment = SceneAlignment.CENTER,
        )
    }

    fun signal(text: String, delayAfterMillis: Long): SceneLine {
        return SceneLine(
            text = text,
            reveal = RevealMode.INSTANT,
            style = SceneStyle.SIGNAL,
            delayAfterMillis = delayAfterMillis,
            alignment = SceneAlignment.CENTER,
        )
    }
}
