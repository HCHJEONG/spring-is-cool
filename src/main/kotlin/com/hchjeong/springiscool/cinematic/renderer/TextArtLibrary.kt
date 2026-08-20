package com.hchjeong.springiscool.cinematic.renderer

object TextArtLibrary {
    private val assets = mapOf(
        "telephone" to TextArtAsset(
            id = "telephone",
            text = "\u260E",
            minimumWidth = 20,
            preferredWidth = 80,
            tags = setOf("office", "signal", "call"),
            semanticMeaning = "A ringing desk telephone.",
        ),
        "signal-marker" to TextArtAsset(
            id = "signal-marker",
            text = "!",
            minimumWidth = 20,
            preferredWidth = 80,
            tags = setOf("warning", "signal"),
            semanticMeaning = "A compact attention marker.",
        ),
    )

    fun telephone(style: SceneStyle = SceneStyle.SIGNAL, delayAfterMillis: Long = 700): SceneLine {
        return assetLine(
            id = "telephone",
            style = style,
            delayAfterMillis = delayAfterMillis,
        )
    }

    fun asset(id: String): TextArtAsset? {
        return assets[id]
    }

    fun assetNames(): List<String> {
        return assets.keys.sorted()
    }

    fun assetLine(id: String, style: SceneStyle = SceneStyle.SIGNAL, delayAfterMillis: Long = 700): SceneLine {
        val asset = requireNotNull(asset(id)) { "Unknown text art asset: $id" }

        return SceneLine(
            text = asset.text,
            reveal = RevealMode.INSTANT,
            style = style,
            delayAfterMillis = delayAfterMillis,
            alignment = SceneAlignment.CENTER,
        )
    }

    fun signal(text: String, delayAfterMillis: Long): SceneLine {
        return SceneLine(
            text = text,
            reveal = RevealMode.INSTANT,
            style = SceneStyle.SIGNAL,
            delayAfterMillis = delayAfterMillis,
            alignment = SceneAlignment.CENTER,
        )
    }
}

data class TextArtAsset(
    val id: String,
    val text: String,
    val minimumWidth: Int,
    val preferredWidth: Int,
    val tags: Set<String>,
    val semanticMeaning: String,
)
