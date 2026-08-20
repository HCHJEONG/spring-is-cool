package com.hchjeong.springiscool.adapter.ssh

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class EmbeddedSshServer(
    private val properties: SshServerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private var sshServer: SshServer? = null

    @PostConstruct
    fun start() {
        if (!properties.enabled) {
            log.info("Embedded SSH server is disabled")
            return
        }

        val hostKeyPath = Path.of("build", "ssh", "hostkey.ser")
        Files.createDirectories(hostKeyPath.parent)

        val server = SshServer.setUpDefaultServer()
        server.host = properties.host
        server.port = properties.port
        server.keyPairProvider = SimpleGeneratorHostKeyProvider(hostKeyPath)

        server.passwordAuthenticator = PasswordAuthenticator { username, password, _ ->
            username == properties.demoUser && password == properties.demoPassword
        }

        server.start()
        sshServer = server

        log.info(
            "Embedded SSH server started on {}:{} with demo user '{}'",
            properties.host,
            properties.port,
            properties.demoUser,
        )
    }

    @PreDestroy
    fun stop() {
        val server = sshServer ?: return

        log.info("Stopping embedded SSH server")
        server.stop(true)
        sshServer = null
    }
}