package com.rakshax.app.data.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencySmsHelperTest {

    @Test
    fun testBuildConfirmationUrl() {
        val url = EmergencySmsHelper.buildConfirmationUrl("evt_12345", "Rahul Sharma")
        assertTrue("URL should contain event ID", url.contains("/sos/evt_12345/acknowledge"))
        assertTrue("URL should contain encoded contact name", url.contains("contact_name=Rahul+Sharma") || url.contains("contact_name=Rahul%20Sharma"))
    }

    @Test
    fun testBuildEmergencyMessageWithLocation() {
        val message = EmergencySmsHelper.buildEmergencyMessage(
            victimName = "Aarav Sharma",
            contactName = "Rahul Sharma",
            eventId = "evt_999",
            locationText = "Connaught Place, New Delhi"
        )
        assertTrue("Message should include victim name", message.contains("Aarav Sharma"))
        assertTrue("Message should include location", message.contains("Location: Connaught Place, New Delhi"))
        assertTrue("Message should include confirmation link", message.contains("/sos/evt_999/acknowledge"))
    }

    @Test
    fun testBuildEmergencyMessageWithoutLocation() {
        val message = EmergencySmsHelper.buildEmergencyMessage(
            victimName = "Aarav Sharma",
            contactName = "Rahul Sharma",
            eventId = "evt_999",
            locationText = null
        )
        assertTrue("Message should include victim name", message.contains("Aarav Sharma"))
        assertTrue("Message should include confirmation link", message.contains("/sos/evt_999/acknowledge"))
    }
}
