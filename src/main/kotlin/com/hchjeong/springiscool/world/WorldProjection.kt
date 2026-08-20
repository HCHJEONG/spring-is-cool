package com.hchjeong.springiscool.world

data class WorldProjection(
    val telephone: TelephoneProjection,
    val presence: PresenceSnapshot,
    val activeInstruction: String?,
    val instructionCompleted: Boolean,
    val lastDelegation: DelegationProjection?,
    val lastEvidenceId: String?,
    val eventCount: Int,
) {
    val facts: List<String> = buildList {
        add("telephone=${telephone.state.name}")
        add("line=${telephone.lineState.name}")
        add("users=${presence.activeUserCount}")
        add("user-authority=${presence.userRole.authority.joinToString("|")}")
        presence.agents.forEach { add("agent=${it.displayName}:${it.state}") }
        presence.agents.forEach {
            add("agent-authority=${it.displayName}:${it.authority.joinToString("|")}")
        }
        activeInstruction?.let {
            add("instruction=$it")
            add("instruction-status=${if (instructionCompleted) "COMPLETED" else "NOT_COMPLETED"}")
        }
        lastDelegation?.let {
            add("delegation=${it.actorId}:${it.task}:${it.authority.name}")
        }
        lastEvidenceId?.let { add("evidence=$it") }
        add("events=$eventCount")
    }

    companion object {
        fun from(session: WorldSession, presence: PresenceSnapshot = PresenceSnapshot()): WorldProjection {
            return from(session.history(), presence)
        }

        fun from(events: List<WorldEvent>, presence: PresenceSnapshot = PresenceSnapshot()): WorldProjection {
            val latestTelephoneEvent = events.lastOrNull {
                it.action == WorldAction.IncomingCall ||
                    it.action == WorldAction.AnsweredTelephone ||
                    it.action == WorldAction.LineWentOffline
            }
            val ringing = latestTelephoneEvent == null || latestTelephoneEvent.action == WorldAction.IncomingCall
            val answered = latestTelephoneEvent?.action == WorldAction.AnsweredTelephone
            val offline = latestTelephoneEvent?.action == WorldAction.LineWentOffline
            val latestAssignment = events.lastOrNull { it.action == WorldAction.AssignedTask }
            val latestEvidence = events.lastOrNull { it.action == WorldAction.EvidenceAttached }
            val instructionEvent = events.lastOrNull { it.action == WorldAction.LineWentOffline }
            val instructionCompleted = instructionEvent?.let { instruction ->
                events.any { it.action == WorldAction.Looked && it.sequence > instruction.sequence }
            } ?: false

            return WorldProjection(
                telephone = TelephoneProjection(
                    state = when {
                        ringing -> TelephoneState.RINGING
                        else -> TelephoneState.SILENT
                    },
                    lineState = when {
                        offline -> LineState.OFFLINE
                        answered -> LineState.OPEN
                        else -> LineState.UNANSWERED
                    },
                ),
                presence = presence,
                activeInstruction = if (offline) {
                    "Look around the office. Type LOOK."
                } else {
                    null
                },
                instructionCompleted = instructionCompleted,
                lastDelegation = latestAssignment?.let {
                    DelegationProjection(
                        actorId = it.targetActorId ?: "unknown",
                        task = it.observation.text,
                        authority = it.authorityResult ?: AuthorityResult.DENIED,
                    )
                },
                lastEvidenceId = latestEvidence?.evidenceId,
                eventCount = events.size,
            )
        }
    }
}

data class TelephoneProjection(
    val state: TelephoneState,
    val lineState: LineState,
)

data class DelegationProjection(
    val actorId: String,
    val task: String,
    val authority: AuthorityResult,
)

enum class TelephoneState {
    RINGING,
    SILENT,
}

enum class LineState {
    UNANSWERED,
    OPEN,
    OFFLINE,
}
