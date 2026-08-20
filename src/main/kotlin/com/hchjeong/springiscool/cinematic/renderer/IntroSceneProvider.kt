package com.hchjeong.springiscool.cinematic.renderer

import org.springframework.stereotype.Component

@Component
class IntroSceneProvider {
    fun welcomeLines(): List<String> {
        return listOf(
            "Welcome to spring-is-cool.",
        )
    }
}