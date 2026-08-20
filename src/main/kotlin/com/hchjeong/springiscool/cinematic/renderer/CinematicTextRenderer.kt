package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component
import java.io.OutputStream

@Component
class CinematicTextRenderer(
    private val introSceneProvider: IntroSceneProvider,
) {
    fun renderIntro(output: OutputStream) {
        val terminal = TerminalOutput(output)

        terminal.clearScreen()
        terminal.hideCursor()

        try {
            introSceneProvider.welcomeLines().forEach { line ->
                if (Thread.currentThread().isInterrupted) {
                    return
                }

                when (line.reveal) {
                    RevealMode.INSTANT -> terminal.writeStyledLine(line.text, line.style)
                    RevealMode.TYPEWRITER -> writeSlowly(terminal, line)
                }

                Thread.sleep(line.delayAfterMillis)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return
        } finally {
            terminal.showCursor()
        }

        terminal.writeLine()
        terminal.prompt()
    }

    private fun writeSlowly(terminal: TerminalOutput, line: IntroSceneLine) {
        line.text.forEach { char ->
            if (Thread.currentThread().isInterrupted) {
                return
            }

            terminal.writeStyled(char.toString(), line.style)
            Thread.sleep(delayAfterCharacterMillis(char, line.characterDelayMillis))
        }

        terminal.newLine()
    }

    private fun delayAfterCharacterMillis(char: Char, defaultDelayMillis: Long): Long {
        return when (char) {
            '.', ',', ';', ':' -> defaultDelayMillis + 145
            else -> defaultDelayMillis
        }
    }

    fun renderCommandResponse(output: OutputStream, line: String) {
        val terminal = TerminalOutput(output)

        if (line.isNotEmpty()) {
            terminal.writeStyledLine("You said: $line", SceneStyle.SYSTEM)
        }

        terminal.prompt()
    }

    fun renderGoodbye(output: OutputStream) {
        val terminal = TerminalOutput(output)
        terminal.writeStyledLine("Bye.", SceneStyle.SYSTEM)
    }
}
