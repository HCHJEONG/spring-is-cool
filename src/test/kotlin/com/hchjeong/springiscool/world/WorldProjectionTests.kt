package com.hchjeong.springiscool.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldProjectionTests {
    @Test
    fun `projects initial ringing telephone state`() {
        val projection = WorldProjection.from(WorldSession())

        assertEquals(TelephoneState.RINGING, projection.telephone.state)
        assertEquals(LineState.UNANSWERED, projection.telephone.lineState)
        assertEquals(null, projection.activeInstruction)
        assertTrue(projection.facts.contains("telephone=RINGING"))
        assertTrue(projection.facts.contains("line=UNANSWERED"))
        assertTrue(projection.facts.contains("users=0"))
        assertTrue(projection.facts.contains("agent=AI clerk:STANDBY"))
        assertTrue(projection.facts.any { it.contains("user-authority=inspect office") })
        assertTrue(projection.facts.any { it.contains("agent-authority=AI clerk:check line state") })
    }

    @Test
    fun `projects answered call as offline line with instruction`() {
        val session = WorldSession()

        session.answerTelephone()
        session.record(
            action = WorldAction.AnsweredTelephone,
            observation = "The operator answered the ringing telephone and received the caller's instruction.",
        )
        session.record(
            action = WorldAction.LineWentOffline,
            observation = "The caller said goodbye and the telephone line went offline.",
        )

        val projection = WorldProjection.from(session)

        assertEquals(TelephoneState.SILENT, projection.telephone.state)
        assertEquals(LineState.OFFLINE, projection.telephone.lineState)
        assertEquals("Look around the office. Type LOOK.", projection.activeInstruction)
        assertTrue(projection.facts.contains("line=OFFLINE"))
        assertTrue(projection.facts.any { it.startsWith("instruction=Look around") })
    }

    @Test
    fun `projects delegation and evidence facts`() {
        val session = WorldSession()

        session.record(
            action = WorldAction.AssignedTask,
            observation = "The operator delegated `check line` to AI clerk.",
            targetActorId = "ai-clerk",
            authorityResult = AuthorityResult.ALLOWED,
        )
        session.record(
            action = WorldAction.EvidenceAttached,
            observation = "Evidence `carrier-tone-present` attached to the line check.",
            actor = Actor("ai-clerk"),
            evidenceId = "carrier-tone-present",
        )

        val projection = WorldProjection.from(session)

        assertEquals("ai-clerk", projection.lastDelegation?.actorId)
        assertEquals(AuthorityResult.ALLOWED, projection.lastDelegation?.authority)
        assertEquals("carrier-tone-present", projection.lastEvidenceId)
        assertTrue(projection.facts.any { it.contains("delegation=ai-clerk") })
        assertTrue(projection.facts.contains("evidence=carrier-tone-present"))
    }
}
