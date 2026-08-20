package com.hchjeong.springiscool.persistence

import com.hchjeong.springiscool.world.WorldAction
import com.hchjeong.springiscool.world.WorldSession
import com.hchjeong.springiscool.world.Actor
import com.hchjeong.springiscool.world.AuthorityResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SQLiteWorldEventStoreTests {
    @Test
    fun `stores and loads world events`() {
        val database = Files.createTempFile("spring-is-cool-world-", ".sqlite")
        val store = SQLiteWorldEventStore(
            PersistenceProperties(sqlitePath = database.toString()),
        )
        store.initialize()

        val session = WorldSession(sessionId = "test-office", eventStore = store)
        session.record(WorldAction.Looked, "The operator looked around.")

        val loaded = store.load("test-office")

        assertEquals(2, loaded.size)
        assertEquals("SYSTEM", loaded[0].actionVerb)
        assertEquals("LOOK", loaded[1].actionVerb)
        assertEquals("The operator looked around.", loaded[1].observationText)
    }

    @Test
    fun `world session restores answered line from stored history`() {
        val database = Files.createTempFile("spring-is-cool-world-", ".sqlite")
        val store = SQLiteWorldEventStore(
            PersistenceProperties(sqlitePath = database.toString()),
        )
        store.initialize()

        val first = WorldSession(sessionId = "test-office", eventStore = store)
        first.answerTelephone()
        first.record(WorldAction.AnsweredTelephone, "The operator answered the ringing telephone.")
        first.record(WorldAction.LineWentOffline, "The caller said goodbye and the telephone line went offline.")

        val restored = WorldSession(
            sessionId = "test-office",
            eventStore = store,
            storedEvents = store.load("test-office"),
        )

        assertTrue(restored.lineAnswered)
        assertTrue(restored.lineOffline)
        assertFalse(restored.telephoneRinging)
        assertEquals(first.history().size, restored.history().size)
    }

    @Test
    fun `stores and loads delegation metadata`() {
        val database = Files.createTempFile("spring-is-cool-world-", ".sqlite")
        val store = SQLiteWorldEventStore(
            PersistenceProperties(sqlitePath = database.toString()),
        )
        store.initialize()

        val session = WorldSession(sessionId = "test-office", eventStore = store)
        session.record(
            action = WorldAction.AssignedTask,
            observation = "The operator delegated `check line` to AI clerk.",
            targetActorId = "ai-clerk",
            authorityResult = AuthorityResult.ALLOWED,
        )
        session.record(
            action = WorldAction.EvidenceAttached,
            observation = "Evidence attached.",
            actor = Actor("ai-clerk"),
            evidenceId = "carrier-tone-present",
        )

        val loaded = store.load("test-office")

        assertEquals("ai-clerk", loaded[1].targetActorId)
        assertEquals("ALLOWED", loaded[1].authorityResult)
        assertEquals("ai-clerk", loaded[2].actorId)
        assertEquals("carrier-tone-present", loaded[2].evidenceId)
    }
}
