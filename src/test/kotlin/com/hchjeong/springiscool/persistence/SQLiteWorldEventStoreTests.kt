package com.hchjeong.springiscool.persistence

import com.hchjeong.springiscool.world.WorldAction
import com.hchjeong.springiscool.world.WorldSession
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

        val restored = WorldSession(
            sessionId = "test-office",
            eventStore = store,
            storedEvents = store.load("test-office"),
        )

        assertTrue(restored.lineAnswered)
        assertFalse(restored.telephoneRinging)
        assertEquals(first.history().size, restored.history().size)
    }
}
