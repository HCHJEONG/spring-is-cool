package com.hchjeong.springiscool.ai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

interface GoogleAccessTokenProvider {
    fun accessToken(): String
}

@Component
@ConditionalOnProperty(prefix = "spring-is-cool.ai", name = ["provider"], havingValue = "gemini")
class ServiceAccountAccessTokenProvider : GoogleAccessTokenProvider {
    private val objectMapper = jacksonObjectMapper()
    private val restClient = RestClient.create()
    private var cached: CachedToken? = null

    override fun accessToken(): String {
        cached?.takeIf { it.expiresAt.isAfter(Instant.now().plusSeconds(60)) }?.let {
            return it.value
        }

        val credentials = readCredentials()
        val jwt = signedJwt(credentials)
        val body = form(
            "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion" to jwt,
        )

        val response = restClient.post()
            .uri(credentials.tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(String::class.java)
            ?: error("Google token endpoint returned an empty body.")

        val json = objectMapper.readTree(response)
        val token = json.path("access_token").asText()
        val expiresIn = json.path("expires_in").asLong(3600)
        require(token.isNotBlank()) { "Google token endpoint did not return access_token." }

        return token.also {
            cached = CachedToken(it, Instant.now().plusSeconds(expiresIn))
        }
    }

    private fun readCredentials(): ServiceAccountCredentials {
        val path = System.getenv("GOOGLE_APPLICATION_CREDENTIALS").orEmpty()
        require(path.isNotBlank()) { "GOOGLE_APPLICATION_CREDENTIALS is required." }

        val root = Files.newBufferedReader(Path.of(path)).use { objectMapper.readTree(it) }
        return ServiceAccountCredentials(
            clientEmail = root.path("client_email").asText(),
            privateKey = root.path("private_key").asText(),
            tokenUri = root.path("token_uri").asText("https://oauth2.googleapis.com/token"),
        )
    }

    private fun signedJwt(credentials: ServiceAccountCredentials): String {
        val now = Instant.now().epochSecond
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
        val claim = base64Url(
            objectMapper.writeValueAsBytes(
                mapOf(
                    "iss" to credentials.clientEmail,
                    "scope" to "https://www.googleapis.com/auth/cloud-platform",
                    "aud" to credentials.tokenUri,
                    "iat" to now,
                    "exp" to now + 3600,
                ),
            ),
        )
        val unsigned = "$header.$claim"
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey(credentials.privateKey))
        signature.update(unsigned.toByteArray(StandardCharsets.UTF_8))
        return "$unsigned.${base64Url(signature.sign())}"
    }

    private fun privateKey(pem: String): java.security.PrivateKey {
        val clean = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s+"), "")
        val bytes = Base64.getDecoder().decode(clean)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun form(vararg pairs: Pair<String, String>): String {
        return pairs.joinToString("&") {
            "${url(it.first)}=${url(it.second)}"
        }
    }

    private fun url(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }

    private fun base64Url(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

private data class ServiceAccountCredentials(
    val clientEmail: String,
    val privateKey: String,
    val tokenUri: String,
)

private data class CachedToken(
    val value: String,
    val expiresAt: Instant,
)
