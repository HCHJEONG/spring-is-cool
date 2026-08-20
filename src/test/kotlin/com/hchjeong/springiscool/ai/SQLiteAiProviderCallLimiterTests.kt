package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.persistence.PersistenceProperties
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SQLiteAiProviderCallLimiterTests {
    @Test
    fun `persists monthly usage in sqlite and blocks after limit`() {
        val database = Files.createTempFile("spring-is-cool-ai-provider-usage-", ".sqlite")
        val limiter = PersistentAiProviderCallLimiter(
            aiProperties = AiDirectorProperties(monthlyCallLimit = 2),
            usageStore = SQLiteAiProviderUsageStore(
                PersistenceProperties(sqlitePath = database.toString()),
            ),
        )

        val first = limiter.tryAcquire("gemini", "gemini-2.5-flash-lite")
        val second = limiter.tryAcquire("gemini", "gemini-2.5-flash-lite")
        val third = limiter.tryAcquire("gemini", "gemini-2.5-flash-lite")

        assertTrue(first.allowed)
        assertEquals(1, first.used)
        assertTrue(second.allowed)
        assertEquals(2, second.used)
        assertFalse(third.allowed)
        assertEquals(2, third.used)

        DriverManager.getConnection("jdbc:sqlite:$database").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select provider, model_id, used from ai_provider_usage").use { rows ->
                    assertTrue(rows.next())
                    assertEquals("gemini", rows.getString("provider"))
                    assertEquals("gemini-2.5-flash-lite", rows.getString("model_id"))
                    assertEquals(2, rows.getInt("used"))
                }
            }
        }
    }

    @Test
    fun `tracks providers and models separately`() {
        val database = Files.createTempFile("spring-is-cool-ai-provider-usage-", ".sqlite")
        val limiter = PersistentAiProviderCallLimiter(
            aiProperties = AiDirectorProperties(monthlyCallLimit = 1),
            usageStore = SQLiteAiProviderUsageStore(
                PersistenceProperties(sqlitePath = database.toString()),
            ),
        )

        val gemini = limiter.tryAcquire("gemini", "gemini-2.5-flash-lite")
        val openai = limiter.tryAcquire("openai", "gpt-demo")
        val geminiAgain = limiter.tryAcquire("gemini", "gemini-2.5-flash-lite")

        assertTrue(gemini.allowed)
        assertTrue(openai.allowed)
        assertFalse(geminiAgain.allowed)
    }
}
