package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameFeedbackMixTest {
    @Test
    fun zeroMotorsProduceNoVibration() {
        assertEquals(0, mixGameRumbleAmplitude(0, 0))
    }

    @Test
    fun largeMotorRemainsDominantAtEqualInput() {
        val largeOnly = mixGameRumbleAmplitude(255, 0)
        val smallOnly = mixGameRumbleAmplitude(0, 255)

        assertTrue(largeOnly > smallOnly)
    }

    @Test
    fun simultaneousMotorsAddEnergyWithoutOverflow() {
        val largeOnly = mixGameRumbleAmplitude(255, 0)
        val combined = mixGameRumbleAmplitude(255, 255)

        assertTrue(combined > largeOnly)
        assertTrue(combined <= 255)
    }

    @Test
    fun strengthScalesAndClampsOutput() {
        val weak = mixGameRumbleAmplitude(160, 80, 0.5f)
        val normal = mixGameRumbleAmplitude(160, 80, 1.0f)
        val boosted = mixGameRumbleAmplitude(160, 80, 1.5f)
        val overBoosted = mixGameRumbleAmplitude(160, 80, 9.0f)

        assertTrue(weak < normal)
        assertTrue(normal <= boosted)
        assertEquals(boosted, overBoosted)
    }
}
