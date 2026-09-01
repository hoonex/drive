package com.example

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun controllerStateStore_serializesSameProtocolWithoutSnapshotAllocation() {
        val store = ControllerStateStore()
        assertTrue(store.setSteering(-0.25f))
        assertTrue(store.setThrottle(0.8f))
        assertTrue(store.setBrake(0.4f))
        assertTrue(store.setClutch(0.2f))
        assertTrue(store.setHandbrake(1f))
        assertTrue(store.setButton(ControllerButtonBits.SHIFT_DOWN, true))
        assertTrue(store.setButton(ControllerButtonBits.CAMERA, true))
        assertFalse(store.setButton(ControllerButtonBits.CAMERA, true))

        val bytes = ByteArray(36)
        val writer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        store.writeTo(writer, 42, 777L)

        assertEquals(36, writer.position())
        val packet = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(42, packet.getInt(0))
        assertEquals(777L, packet.getLong(4))
        assertEquals(-0.25f, packet.getFloat(12), 0f)
        assertEquals(0.8f, packet.getFloat(16), 0f)
        assertEquals(0.4f, packet.getFloat(20), 0f)
        assertEquals(0.2f, packet.getFloat(24), 0f)
        assertEquals(1f, packet.getFloat(28), 0f)
        assertEquals(
            ControllerButtonBits.SHIFT_DOWN or ControllerButtonBits.CAMERA,
            packet.getInt(32),
        )

        val snapshot = store.snapshot()
        assertEquals(-0.25f, snapshot.steering, 0f)
        assertEquals(0.8f, snapshot.throttle, 0f)
        assertTrue(snapshot.shiftDown)
        assertTrue(snapshot.camera)
        assertFalse(snapshot.shiftUp)
    }

    @Test
    fun controllerStateStore_clampsAndResetsAllInputs() {
        val store = ControllerStateStore()
        store.setSteering(5f)
        store.setThrottle(-2f)
        store.setBrake(3f)
        store.setClutch(0.7f)
        store.setHandbrake(9f)
        store.setButton(ControllerButtonBits.RESET, true)

        val clamped = store.snapshot()
        assertEquals(1f, clamped.steering, 0f)
        assertEquals(0f, clamped.throttle, 0f)
        assertEquals(1f, clamped.brake, 0f)
        assertEquals(0.7f, clamped.clutch, 0f)
        assertEquals(1f, clamped.handbrake, 0f)
        assertTrue(clamped.reset)

        store.reset()
        assertEquals(ControllerState(), store.snapshot())
    }
}
