package com.hchjeong.springiscool.presentation

import com.hchjeong.springiscool.cinematic.renderer.Scene
import com.hchjeong.springiscool.world.CommandResult
import com.hchjeong.springiscool.world.WorldCommandHandler
import com.hchjeong.springiscool.world.WorldSession
import org.springframework.stereotype.Component

@Component
class WorldInteractionService(
    private val commandHandler: WorldCommandHandler,
    private val scenePresenter: ScenePresenter,
) {
    fun submit(session: WorldSession, commandText: String): WorldInteractionResult {
        return when (val result = commandHandler.handle(session, commandText)) {
            is CommandResult.Continue -> present(InteractionOutcome.CONTINUE, result.scene)
            is CommandResult.Quit -> present(InteractionOutcome.QUIT, result.scene)
        }
    }

    private fun present(outcome: InteractionOutcome, scene: Scene): WorldInteractionResult {
        return WorldInteractionResult(
            outcome = outcome,
            rendererScene = scene,
            presentedScene = scenePresenter.present(scene),
        )
    }
}

data class WorldInteractionResult(
    val outcome: InteractionOutcome,
    val rendererScene: Scene,
    val presentedScene: PresentedScene,
)

enum class InteractionOutcome {
    CONTINUE,
    QUIT,
}
