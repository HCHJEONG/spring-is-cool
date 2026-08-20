package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.persistence.WorldSessionFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/world")
class WorldCommandController(
    private val worldSessionFactory: WorldSessionFactory,
    private val worldInteractionService: WorldInteractionService,
) {
    @PostMapping("/commands")
    fun submit(@RequestBody request: WorldCommandRequest): ResponseEntity<WorldCommandResponse> {
        val session = worldSessionFactory.create()
        val result = worldInteractionService.submit(session, request.command)

        return ResponseEntity.ok(
            WorldCommandResponse(
                sessionId = session.sessionId,
                outcome = result.outcome.name.lowercase(),
                scene = result.presentedScene,
            ),
        )
    }
}

data class WorldCommandRequest(
    val command: String = "",
)

data class WorldCommandResponse(
    val sessionId: String,
    val outcome: String,
    val scene: PresentedScene,
)
