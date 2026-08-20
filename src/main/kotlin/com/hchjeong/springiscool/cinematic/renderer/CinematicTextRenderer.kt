package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component
import java.io.OutputStream

@Component
class CinematicTextRenderer(
    private val introSceneProvider: IntroSceneProvider,
) {
    fun renderIntro(output: OutputStream) {
        val terminal = TerminalOutput(output)

        introSceneProvider.welcomeLines().forEach { line ->
            terminal.writeLine(line)
        }

        terminal.writeLine()
        terminal.prompt()
    }

    fun renderCommandResponse(output: OutputStream, line: String) {
        val terminal = TerminalOutput(output)

        if (line.isNotEmpty()) {
            terminal.writeLine("You said: $line")
        }

        terminal.prompt()
    }

    fun renderGoodbye(output: OutputStream) {
        val terminal = TerminalOutput(output)
        terminal.writeLine("Bye.")
    }
}