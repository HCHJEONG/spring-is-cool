package com.hchjeong.springiscool.cinematic.renderer

class TerminalCanvas(
    private val width: Int,
) {
    init {
        require(width > 0) { "Canvas width must be positive." }
    }

    fun padFor(text: String, alignment: SceneAlignment): String {
        val textWidth = TerminalTextWidth.visibleWidth(text)
        val padding = when (alignment) {
            SceneAlignment.LEFT -> 0
            SceneAlignment.CENTER -> ((width - textWidth) / 2).coerceAtLeast(0)
            SceneAlignment.RIGHT -> (width - textWidth).coerceAtLeast(0)
        }

        return " ".repeat(padding)
    }
}
