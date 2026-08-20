package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component
import java.io.OutputStream

@Component
class CinematicTextRenderer(
    private val introSceneProvider: IntroSceneProvider,
) {
    fun renderIntro(output: OutputStream) {
        render(output, introSceneProvider.welcomeScene())
    }

    fun render(
        output: OutputStream,
        scene: Scene,
        theme: TerminalTheme = GreenCrtTerminalTheme,
        timingProfile: TimingProfile = TimingProfile.CINEMATIC,
    ) {
        val terminal = TerminalOutput(output, theme)
        val canvas = TerminalCanvas(scene.terminalWidth)

        if (scene.clearBefore) {
            terminal.clearScreen()
        }

        if (scene.hideCursorDuringPlayback) {
            terminal.hideCursor()
        }

        try {
            scene.lines.forEach { line ->
                if (Thread.currentThread().isInterrupted) {
                    return
                }

                renderLine(terminal, canvas, line, timingProfile)
                sleep(timingProfile.lineDelay(line.delayAfterMillis))
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return
        } finally {
            if (scene.hideCursorDuringPlayback) {
                terminal.showCursor()
            }
        }

        if (scene.showPromptAfter) {
            terminal.writeLine()
            terminal.prompt()
        }
    }

    private fun renderLine(
        terminal: TerminalOutput,
        canvas: TerminalCanvas,
        line: SceneLine,
        timingProfile: TimingProfile,
    ) {
        terminal.write(canvas.padFor(line.text, line.alignment))

        when (line.reveal) {
            RevealMode.INSTANT -> terminal.writeStyledLine(line.text, line.style)
            RevealMode.TYPEWRITER -> writeSlowly(terminal, line, timingProfile)
        }
    }

    private fun writeSlowly(terminal: TerminalOutput, line: SceneLine, timingProfile: TimingProfile) {
        line.text.forEach { char ->
            if (Thread.currentThread().isInterrupted) {
                return
            }

            terminal.writeStyled(char.toString(), line.style)
            sleep(timingProfile.characterDelay(delayAfterCharacterMillis(char, line.characterDelayMillis)))
        }

        terminal.newLine()
    }

    private fun sleep(delayMillis: Long) {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis)
        }
    }

    private fun delayAfterCharacterMillis(char: Char, defaultDelayMillis: Long): Long {
        return when (char) {
            '.', ',', ';', ':' -> defaultDelayMillis + 145
            else -> defaultDelayMillis
        }
    }

}
