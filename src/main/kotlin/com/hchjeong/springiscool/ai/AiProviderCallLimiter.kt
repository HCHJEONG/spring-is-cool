package com.hchjeong.springiscool.ai

import com.hchjeong.springiscool.persistence.PersistenceProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Clock
import java.time.YearMonth

interface AiProviderCallLimiter {
    fun tryAcquire(provider: String, modelId: String): AiProviderCallLimitResult
}

interface AiProviderUsageStore {
    fun currentUsed(provider: String, modelId: String, month: YearMonth): Int
    fun saveUsed(provider: String, modelId: String, month: YearMonth, used: Int)
}

data class AiProviderCallLimitResult(
    val allowed: Boolean,
    val provider: String,
    val modelId: String,
    val month: YearMonth,
    val used: Int,
    val limit: Int,
) {
    val remaining: Int = (limit - used).coerceAtLeast(0)
}

@Component
@ConditionalOnProperty(prefix = "spring-is-cool.ai", name = ["provider"], havingValue = "gemini")
class PersistentAiProviderCallLimiter(
    private val aiProperties: AiDirectorProperties,
    private val usageStore: AiProviderUsageStore,
) : AiProviderCallLimiter {
    private val clock: Clock = Clock.systemUTC()

    override fun tryAcquire(provider: String, modelId: String): AiProviderCallLimitResult {
        synchronized(this) {
            val limit = aiProperties.monthlyCallLimit
            val month = YearMonth.now(clock)
            if (limit <= 0) {
                return AiProviderCallLimitResult(
                    allowed = false,
                    provider = provider,
                    modelId = modelId,
                    month = month,
                    used = 0,
                    limit = limit,
                )
            }

            val currentUsed = usageStore.currentUsed(provider, modelId, month)
            if (currentUsed >= limit) {
                return AiProviderCallLimitResult(
                    allowed = false,
                    provider = provider,
                    modelId = modelId,
                    month = month,
                    used = currentUsed,
                    limit = limit,
                )
            }

            val nextUsed = currentUsed + 1
            usageStore.saveUsed(provider, modelId, month, nextUsed)
            return AiProviderCallLimitResult(
                allowed = true,
                provider = provider,
                modelId = modelId,
                month = month,
                used = nextUsed,
                limit = limit,
            )
        }
    }
}

@Component
@ConditionalOnProperty(prefix = "spring-is-cool.ai", name = ["provider"], havingValue = "gemini")
class SQLiteAiProviderUsageStore(
    private val persistenceProperties: PersistenceProperties,
) : AiProviderUsageStore {
    override fun currentUsed(provider: String, modelId: String, month: YearMonth): Int {
        initialize()
        return DriverManager.getConnection(jdbcUrl()).use { connection ->
            connection.prepareStatement(
                "select used from ai_provider_usage where provider = ? and model_id = ? and month = ?",
            ).use { statement ->
                statement.setString(1, provider)
                statement.setString(2, modelId)
                statement.setString(3, month.toString())
                statement.executeQuery().use { rows ->
                    if (rows.next()) rows.getInt("used") else 0
                }
            }
        }
    }

    override fun saveUsed(provider: String, modelId: String, month: YearMonth, used: Int) {
        initialize()
        DriverManager.getConnection(jdbcUrl()).use { connection ->
            connection.prepareStatement(
                """
                insert into ai_provider_usage (provider, model_id, month, used)
                values (?, ?, ?, ?)
                on conflict(provider, model_id, month) do update set used = excluded.used
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, provider)
                statement.setString(2, modelId)
                statement.setString(3, month.toString())
                statement.setInt(4, used)
                statement.executeUpdate()
            }
        }
    }

    private fun initialize() {
        val database = Path.of(persistenceProperties.sqlitePath)
        database.parent?.let { Files.createDirectories(it) }
        DriverManager.getConnection(jdbcUrl()).use { connection ->
            connection.createStatement().use {
                it.executeUpdate(
                    """
                    create table if not exists ai_provider_usage (
                      provider text not null,
                      model_id text not null,
                      month text not null,
                      used integer not null,
                      primary key (provider, model_id, month)
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    private fun jdbcUrl(): String {
        return "jdbc:sqlite:${persistenceProperties.sqlitePath}"
    }
}
