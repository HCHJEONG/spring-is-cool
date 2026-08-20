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

    fun prompt() {
        write("> ")
    }
}