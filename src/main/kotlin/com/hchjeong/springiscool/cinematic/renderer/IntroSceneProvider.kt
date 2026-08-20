package com.hchjeong.springiscool.cinematic.renderer

import com.hchjeong.springiscool.cinematic.scene.SceneDefinitionLoader
import org.springframework.stereotype.Component

@Component
class IntroSceneProvider(
    private val sceneDefinitionLoader: SceneDefinitionLoader = SceneDefinitionLoader(),
) {
    fun welcomeScene(): Scene {
        return sceneDefinitionLoader.loadResource("scenes/ontoloffice-intro.json")
    }
}
