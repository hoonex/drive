package com.example

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerPacketTest {
    @Test
    fun controllerPacket_matchesWindowsReceiverProtocolExactly() {
        val bytes = ByteArray(36)
        val writer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val sequence = 0x10203040
        val timestamp = 1_234_567_890_123L

        ControllerState(
            steering = -0.5f,
            throttle = 0.75f,
            brake = 0.25f,
            clutch = 1.0f,
            handbrake = 0.5f,
            shiftUp = true,
            shiftDown = false,
            horn = true,
            camera = false,
            reset = true,
        ).writeTo(writer, sequence, timestamp)

        assertEquals(36, writer.position())

        val packet = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(sequence, packet.getInt(0))
        assertEquals(timestamp, packet.getLong(4))
        assertEquals(-0.5f, packet.getFloat(12), 0f)
        assertEquals(0.75f, packet.getFloat(16), 0f)
        assertEquals(0.25f, packet.getFloat(20), 0f)
        assertEquals(1.0f, packet.getFloat(24), 0f)
        assertEquals(0.5f, packet.getFloat(28), 0f)
        assertEquals(0b1_0101, packet.getInt(32))
    }

    @Test
    fun controllerPacket_clampsAnalogValuesWithoutChangingLayout() {
        val bytes = ByteArray(36)
        val writer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        ControllerState(
            steering = 4f,
            throttle = -1f,
            brake = 2f,
            clutch = -3f,
            handbrake = 5f,
        ).writeTo(writer, 7, 99L)

        val packet = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(1f, packet.getFloat(12), 0f)
        assertEquals(0f, packet.getFloat(16), 0f)
        assertEquals(1f, packet.getFloat(20), 0f)
        assertEquals(0f, packet.getFloat(24), 0f)
        assertEquals(1f, packet.getFloat(28), 0f)
        assertEquals(0, packet.getInt(32))
    }
}
