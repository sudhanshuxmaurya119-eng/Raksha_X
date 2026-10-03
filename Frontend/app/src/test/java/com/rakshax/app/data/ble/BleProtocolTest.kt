package com.rakshax.app.data.ble

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleProtocolTest {
    @Test
    fun acceptsOnlyTheTrimmedSosSignal() {
        assertTrue(BleProtocol.isSosSignal("SOS".toByteArray()))
        assertTrue(BleProtocol.isSosSignal(" sos ".toByteArray()))
        assertFalse(BleProtocol.isSosSignal("READY".toByteArray()))
        assertFalse(BleProtocol.isSosSignal("SOS_TEST".toByteArray()))
    }
}
