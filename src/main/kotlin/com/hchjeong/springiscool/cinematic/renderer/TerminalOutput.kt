package com.hchjeong.springiscool.cinematic.renderer

import java.io.OutputStream
import java.nio.charset.StandardCharsets

class TerminalOutput(
    private val output: OutputStream,
) {
    fun write(text: String) {
        output.write(text.toByteArray(StandardCharsets.UTF_8))
        output.flush()
    }

    fun writeLine(text: String = "") {
        write("$text\r\n")
    }

    fun writeStyled(text: String, style: SceneStyle) {
        write("${ansiFor(style)}$text$RESET")
    }

    fun writeStyledLine(text: String = "", style: SceneStyle) {
        if (text.isEmpty()) {
            writeLine()
            return
        }

        writeStyled(text, style)
        newLine()
    }

    fun prompt() {
        writeStyled("> _", SceneStyle.PROMPT)
        write("\b")
    }

    fun clearScreen() {
        write("\u001B[2J\u001B[H")
    }

    fun hideCursor() {
        write("\u001B[?25l")
    }

    fun showCursor() {
        write("\u001B[?25h")
    }

    fun newLine() {
        write("\r\n")
    }

    fun erasePromptPlaceholder() {
        write(" \b")
    }

    fun erasePreviousCharacter() {
        write("\b \b")
    }

    fun restorePromptPlaceholder() {
        write("_\b")
    }

    private fun ansiFor(style: SceneStyle): String {
        return when (style) {
            SceneStyle.NARRATION -> "\u001B[38;5;114m"
            SceneStyle.SIGNAL -> "\u001B[38;5;191;1m"
            SceneStyle.PROMPT -> "\u001B[38;5;120;1m"
            SceneStyle.SYSTEM -> "\u001B[38;5;81m"
        }
    }

    companion object {
        private const val RESET = "\u001B[0m"
    }
}
