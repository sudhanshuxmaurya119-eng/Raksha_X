package com.rakshax.app.data.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SosEscalationPolicyTest {
    @Test
    fun advancesThroughPriorityStages() {
        assertEquals(2, SosEscalationPolicy.nextStage(1, acknowledged = false))
        assertEquals(3, SosEscalationPolicy.nextStage(2, acknowledged = false))
        assertEquals(2, SosEscalationPolicy.pendingContacts(1))
        assertEquals(0, SosEscalationPolicy.pendingContacts(3))
    }

    @Test
    fun acknowledgementOrFinalStageStopsEscalation() {
        assertNull(SosEscalationPolicy.nextStage(1, acknowledged = true))
        assertNull(SosEscalationPolicy.nextStage(3, acknowledged = false))
    }
}
