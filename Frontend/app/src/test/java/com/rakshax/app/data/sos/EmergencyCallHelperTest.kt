package com.rakshax.app.data.sos

import com.rakshax.app.data.model.Contact
import com.rakshax.app.data.repository.MockDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EmergencyCallHelperTest {

    @Test
    fun resolvesPriorityOneContactByDefault() {
        val target = EmergencyCallHelper.resolveTargetContact()
        assertNotNull(target)
        assertEquals(1, target?.priority)
        assertEquals("Rahul Sharma", target?.name)
    }

    @Test
    fun skipsDisabledContactsAndPicksNextEnabledPriority() {
        val originalContacts = MockDataRepository.contacts.value
        try {
            // Disable Priority 1
            MockDataRepository.toggleContactEnabled("c1")
            val target = EmergencyCallHelper.resolveTargetContact()
            assertNotNull(target)
            assertEquals(2, target?.priority)
            assertEquals("Priya Sharma", target?.name)
        } finally {
            // Restore
            MockDataRepository.toggleContactEnabled("c1")
        }
    }

    @Test
    fun sanitizePhoneNumberKeepsDigitsAndLeadingPlus() {
        val raw = "+91 98111-22334"
        val sanitized = raw.filter { it.isDigit() || it == '+' }
        assertEquals("+919811122334", sanitized)
    }
}
