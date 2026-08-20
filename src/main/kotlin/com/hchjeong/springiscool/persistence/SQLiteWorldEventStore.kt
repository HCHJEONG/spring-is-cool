package com.hchjeong.springiscool.persistence

import com.hchjeong.springiscool.world.WorldAction
import com.hchjeong.springiscool.world.WorldEvent
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

@Component
@ConditionalOnProperty(
    prefix = "spring-is-cool.persistence",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class SQLiteWorldEventStore(
    private val properties: PersistenceProperties,
) : WorldEventStore {
    private val databasePath: Path = Path.of(properties.sqlitePath)

    @PostConstruct
    fun initialize() {
        databasePath.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(SCHEMA)
            }
        }
    }

    override fun append(sessionId: String, event: WorldEvent) {
        connect().use { connection ->
            connection.prepareStatement(
                """
                insert into world_events (
                    session_id,
                    sequence,
                    occurred_at,
                    actor_id,
                    action_verb,
                    action_text,
                    observation_text
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict(session_id, sequence) do nothing
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.setInt(2, event.sequence)
                statement.setString(3, event.occurredAt.toString())
                statement.setString(4, event.actor.id)
                statement.setString(5, event.action.verb)
                statement.setString(6, event.action.detailText())
                statement.setString(7, event.observation.text)
                statement.executeUpdate()
            }
        }
    }

    override fun load(sessionId: String): List<StoredWorldEvent> {
        connect().use { connection ->
            connection.prepareStatement(
                """
                select session_id, sequence, occurred_at, actor_id, action_verb, action_text, observation_text
                from world_events
                where session_id = ?
                order by sequence asc
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, sessionId)
                statement.executeQuery().use { rows ->
                    val events = mutableListOf<StoredWorldEvent>()
                    while (rows.next()) {
                        events += StoredWorldEvent(
                            sessionId = rows.getString("session_id"),
                            sequence = rows.getInt("sequence"),
                            occurredAt = Instant.parse(rows.getString("occurred_at")),
                            actorId = rows.getString("actor_id"),
                            actionVerb = rows.getString("action_verb"),
                            actionText = rows.getString("action_text"),
                            observationText = rows.getString("observation_text"),
                        )
                    }
                    return events
                }
            }
        }
    }

    private fun connect(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${databasePath}")
    }

    private fun WorldAction.detailText(): String? {
        return when (this) {
            is WorldAction.UnknownCommand -> text
            else -> null
        }
    }

    companion object {
        private const val SCHEMA = """
            create table if not exists world_events (
                session_id text not null,
                sequence integer not null,
                occurred_at text not null,
                actor_id text not null,
                action_verb text not null,
                action_text text,
                observation_text text not null,
                primary key (session_id, sequence)
            )
        """
    }
}
