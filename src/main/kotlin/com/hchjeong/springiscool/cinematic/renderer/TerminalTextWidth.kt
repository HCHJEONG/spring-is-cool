package com.hchjeong.springiscool.cinematic.renderer

object TerminalTextWidth {
    fun visibleWidth(text: String): Int {
        var width = 0
        var index = 0

        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            width += widthOf(codePoint)
            index += Character.charCount(codePoint)
        }

        return width
    }

    fun widthOf(codePoint: Int): Int {
        return when {
            codePoint == 0 -> 0
            isCombiningMark(codePoint) -> 0
            isWide(codePoint) -> 2
            else -> 1
        }
    }

    private fun isCombiningMark(codePoint: Int): Boolean {
        val type = Character.getType(codePoint)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt()
    }

    private fun isWide(codePoint: Int): Boolean {
        return codePoint in 0x1100..0x115F ||
            codePoint in 0x2329..0x232A ||
            codePoint in 0x2E80..0xA4CF ||
            codePoint in 0xAC00..0xD7A3 ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0xFE10..0xFE19 ||
            codePoint in 0xFE30..0xFE6F ||
            codePoint in 0xFF00..0xFF60 ||
            codePoint in 0xFFE0..0xFFE6 ||
            codePoint in 0x1F300..0x1FAFF
    }
}
