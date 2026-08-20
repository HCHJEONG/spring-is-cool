package com.hchjeong.springiscool.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

interface GeminiTextClient {
    fun generate(prompt: String): String
}

@Component
@ConditionalOnProperty(prefix = "spring-is-cool.ai", name = ["provider"], havingValue = "gemini")
class VertexGeminiTextClient(
    private val properties: AiDirectorProperties,
    private val accessTokenProvider: GoogleAccessTokenProvider,
) : GeminiTextClient {
    private val objectMapper = jacksonObjectMapper()
    private val restClient = RestClient.builder()
        .requestFactory(org.springframework.http.client.JdkClientHttpRequestFactory().also {
            it.setReadTimeout(Duration.ofMillis(properties.timeoutMillis))
        })
        .build()

    override fun generate(prompt: String): String {
        val endpoint = endpoint()
        val response = restClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .headers { it.setBearerAuth(accessTokenProvider.accessToken()) }
            .body(
                mapOf(
                    "contents" to listOf(
                        mapOf(
                            "role" to "user",
                            "parts" to listOf(mapOf("text" to prompt)),
                        ),
                    ),
                    "generationConfig" to mapOf(
                        "responseMimeType" to "application/json",
                        "temperature" to 0.7,
                    ),
                ),
            )
            .retrieve()
            .body(String::class.java)
            ?: error("Gemini returned an empty body.")

        return extractText(objectMapper.readTree(response))
    }

    private fun endpoint(): String {
        require(properties.project.isNotBlank()) { "GOOGLE_CLOUD_PROJECT is required." }
        require(properties.location.isNotBlank()) { "GOOGLE_CLOUD_LOCATION is required." }
        require(properties.modelId.isNotBlank()) { "VERTEX_AI_MODEL_ID is required." }

        val location = properties.location.trim()
        val host = if (location == "global") {
            "aiplatform.googleapis.com"
        } else {
            "$location-aiplatform.googleapis.com"
        }
        val model = "projects/${properties.project}/locations/$location/publishers/google/models/${properties.modelId}"
        return "https://$host/v1/$model:generateContent"
    }

    private fun extractText(root: JsonNode): String {
        val parts = root.path("candidates").firstOrNull()
            ?.path("content")
            ?.path("parts")
            ?: error("Gemini response did not contain candidates.content.parts.")

        return parts.firstOrNull { it.has("text") }
            ?.path("text")
            ?.asText()
            ?: error("Gemini response did not contain text.")
    }
}
