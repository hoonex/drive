package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteeringResponseTest {
    @Test
    fun deadzone_mapsCenterNoiseToZero() {
        assertEquals(0f, applySteeringResponse(0.01f, 0.02f, 1f, false), 0f)
        assertEquals(0f, applySteeringResponse(-0.019f, 0.02f, 1f, false), 0f)
    }

    @Test
    fun linearResponse_preservesEndsAndSign() {
        assertEquals(1f, applySteeringResponse(1f, 0f, 1f, false), 0f)
        assertEquals(-1f, applySteeringResponse(-1f, 0f, 1f, false), 0f)
        assertEquals(0.5f, applySteeringResponse(0.5f, 0f, 1f, false), 0.0001f)
    }

    @Test
    fun responseCurve_changesMidrangeWithoutChangingFullLock() {
        val quick = applySteeringResponse(0.5f, 0f, 0.7f, false)
        val linear = applySteeringResponse(0.5f, 0f, 1f, false)
        val smooth = applySteeringResponse(0.5f, 0f, 1.5f, false)

        assertTrue(quick > linear)
        assertTrue(smooth < linear)
        assertEquals(1f, applySteeringResponse(1f, 0f, 1.5f, false), 0f)
    }

    @Test
    fun invert_flipsFinalDirection() {
        val normal = applySteeringResponse(0.4f, 0.01f, 1.2f, false)
        val inverted = applySteeringResponse(0.4f, 0.01f, 1.2f, true)
        assertEquals(-normal, inverted, 0.0001f)
    }
}
