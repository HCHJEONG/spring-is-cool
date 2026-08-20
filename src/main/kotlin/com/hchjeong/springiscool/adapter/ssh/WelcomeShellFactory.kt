package com.hchjeong.springiscool.adapter.ssh

import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.shell.ShellFactory
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import com.hchjeong.springiscool.cinematic.renderer.CinematicTextRenderer
import com.hchjeong.springiscool.cinematic.renderer.TerminalOutput
import com.hchjeong.springiscool.persistence.WorldSessionFactory
import com.hchjeong.springiscool.world.CommandResult
import com.hchjeong.springiscool.world.WorldCommandHandler

private fun writeLine(writer: PrintWriter, text: String = "") {
    writer.print("$text\r\n")
    writer.flush()
}

@Component
class WelcomeShellFactory(
    private val renderer: CinematicTextRenderer,
    private val commandHandler: WorldCommandHandler,
    private val worldSessionFactory: WorldSessionFactory,
) : ShellFactory {
    override fun createShell(channel: ChannelSession): Command {
        return WelcomeShellCommand(renderer, commandHandler, worldSessionFactory)
    }
}
private class WelcomeShellCommand(
    private val renderer: CinematicTextRenderer,
    private val commandHandler: WorldCommandHandler,
    private val worldSessionFactory: WorldSessionFactory,
) : Command {
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var error: OutputStream? = null
    private var exitCallback: ExitCallback? = null
    private var worker: Thread? = null

    override fun setInputStream(input: InputStream) {
        this.input = input
    }

    override fun setOutputStream(output: OutputStream) {
        this.output = output
    }

    override fun setErrorStream(error: OutputStream) {
        this.error = error
    }

    override fun setExitCallback(callback: ExitCallback) {
        this.exitCallback = callback
    }

    override fun start(channel: ChannelSession, environment: Environment) {
        worker = Thread {
            runShell()
        }.also {
            it.name = "welcome-ssh-shell"
            it.isDaemon = true
            it.start()
        }
    }

    override fun destroy(channel: ChannelSession) {
        worker?.interrupt()
        worker = null
    }

    private fun runShell() {
        val shellInput = input ?: return
        val shellOutput = output ?: return

        val writer = PrintWriter(shellOutput.writer(StandardCharsets.UTF_8), true)
        val terminal = TerminalOutput(shellOutput)
        val lineBuffer = StringBuilder()
        val worldSession = worldSessionFactory.create()
        var promptPlaceholderVisible = false

        renderer.renderIntro(shellOutput)
        promptPlaceholderVisible = true

        while (!Thread.currentThread().isInterrupted) {
            val next = shellInput.read()
            if (next == -1) {
                break
            }

            val char = next.toChar()

            when (char) {
                '\r', '\n' -> {
                    val line = lineBuffer.toString().trim()
                    lineBuffer.clear()

                    if (promptPlaceholderVisible) {
                        terminal.erasePromptPlaceholder()
                        promptPlaceholderVisible = false
                    }

                    writeLine(writer)

                    when (val result = commandHandler.handle(worldSession, line)) {
                        is CommandResult.Continue -> {
                            renderer.render(shellOutput, result.scene)
                            promptPlaceholderVisible = true
                        }

                        is CommandResult.Quit -> {
                            renderer.render(shellOutput, result.scene)
                            exitCallback?.onExit(0)
                            return
                        }
                    }
                }

                '\b', 127.toChar() -> {
                    if (lineBuffer.isNotEmpty()) {
                        lineBuffer.deleteCharAt(lineBuffer.length - 1)
                        terminal.erasePreviousCharacter()

                        if (lineBuffer.isEmpty()) {
                            terminal.restorePromptPlaceholder()
                            promptPlaceholderVisible = true
                        }
                    }
                }

                else -> {
                    if (promptPlaceholderVisible) {
                        promptPlaceholderVisible = false
                    }

                    lineBuffer.append(char)
                    writer.print(char)
                    writer.flush()
                }
            }
        }

        exitCallback?.onExit(0)
    }
}
