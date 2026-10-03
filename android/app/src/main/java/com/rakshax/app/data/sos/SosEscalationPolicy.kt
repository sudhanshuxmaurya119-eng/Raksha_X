package com.rakshax.app.data.sos

object SosEscalationPolicy {
    const val MAX_STAGE = 3
    const val STAGE_TIMEOUT_MILLIS = 45_000L

    fun nextStage(currentStage: Int, acknowledged: Boolean): Int? {
        if (acknowledged || currentStage >= MAX_STAGE) return null
        return (currentStage + 1).coerceAtMost(MAX_STAGE)
    }

    fun pendingContacts(stage: Int): Int = (MAX_STAGE - stage).coerceAtLeast(0)
}
