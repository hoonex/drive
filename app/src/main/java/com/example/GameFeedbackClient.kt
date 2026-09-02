package com.example

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal fun mixGameRumbleAmplitude(
    largeMotor: Int,
    smallMotor: Int,
    strength: Float = 1f,
): Int {
    val large = largeMotor.coerceIn(0, 255) / 255f
    val small = smallMotor.coerceIn(0, 255) / 255f
    if (large <= 0.001f && small <= 0.001f) return 0

    // XInput exposes two motors but most phones expose one actuator. Weighted RMS keeps
    // large-motor impacts dominant while allowing small-motor texture to add energy
    // instead of being discarded whenever the large motor is stronger.
    val weightedLarge = large * 0.90f
    val weightedSmall = small * 0.72f
    val mixed = sqrt(
        (weightedLarge * weightedLarge + weightedSmall * weightedSmall).toDouble(),
    ).toFloat().coerceAtMost(1f)

    return (mixed * strength.coerceIn(0.25f, 1.5f) * 255f)
        .roundToInt()
        .coerceIn(1, 255)
}

/** Receives XInput rumble relayed by PC Wheel Receiver on a path separate from steering input. */
class GameFeedbackClient(context: Context) : AutoCloseable {
    companion object {
        const val FEEDBACK_PORT = 26762
        private const val PACKET_SIZE = 8
        private const val SOCKET_POLL_MS = 40L
        private const val EFFECT_REFRESH_MS = 70L
        private const val EFFECT_DURATION_MS = 110L
        private const val FEEDBACK_STALE_MS = 180L
        private const val MIN_FORCED_RESTART_MS = 10L
        private const val AMPLITUDE_CHANGE_THRESHOLD = 6
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val canVibrate = vibrator?.hasVibrator() == true

    @Volatile
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopRumble()
        }

    @Volatile
    var strength: Float = 1f

    @Volatile
    private var socket: DatagramSocket? = null

    @Volatile
    private var largeMotor = 0

    @Volatile
    private var smallMotor = 0

    private var lastPacketMs = 0L
    private var lastEffectMs = 0L
    private var lastAppliedAmplitude = 0

    suspend fun start(expectedPcHost: String) = withContext(Dispatchers.IO) {
        closeSocketOnly()
        val expectedAddress = try {
            InetAddress.getByName(expectedPcHost)
        } catch (_: Exception) {
            return@withContext
        }

        val feedbackSocket = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(FEEDBACK_PORT))
                soTimeout = SOCKET_POLL_MS.toInt()
                receiveBufferSize = 16 * 1024
            }
        } catch (_: Exception) {
            return@withContext
        }

        socket = feedbackSocket
        val bytes = ByteArray(PACKET_SIZE)
        val packet = DatagramPacket(bytes, bytes.size)

        try {
            while (currentCoroutineContext().isActive && socket === feedbackSocket) {
                try {
                    packet.length = bytes.size
                    feedbackSocket.receive(packet)
                    if (packet.address != expectedAddress || packet.length != PACKET_SIZE) continue
                    if (
                        bytes[0] != 'P'.code.toByte() ||
                        bytes[1] != 'C'.code.toByte() ||
                        bytes[2] != 'F'.code.toByte() ||
                        bytes[3] != 'B'.code.toByte() ||
                        bytes[4].toInt() != 1
                    ) continue

                    largeMotor = bytes[5].toInt() and 0xFF
                    smallMotor = bytes[6].toInt() and 0xFF
                    lastPacketMs = SystemClock.elapsedRealtime()

                    val amplitude = mixGameRumbleAmplitude(
                        largeMotor = largeMotor,
                        smallMotor = smallMotor,
                        strength = strength,
                    )
                    if (amplitude == 0) {
                        stopRumble()
                    } else {
                        val meaningfulChange =
                            lastAppliedAmplitude == 0 ||
                                abs(amplitude - lastAppliedAmplitude) >= AMPLITUDE_CHANGE_THRESHOLD
                        refreshRumble(force = meaningfulChange)
                    }
                } catch (_: SocketTimeoutException) {
                    // Receiver sends a lightweight heartbeat while game rumble is active.
                    // If that heartbeat disappears, stop quickly instead of letting a stale
                    // non-zero motor state buzz until the main controller link times out.
                    refreshRumble(force = false)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SocketException) {
            // Normal shutdown closes the socket from another coroutine.
        } finally {
            if (socket === feedbackSocket) socket = null
            feedbackSocket.close()
            stopRumble()
        }
    }

    private fun refreshRumble(force: Boolean) {
        if (!enabled || !canVibrate) return

        val now = SystemClock.elapsedRealtime()
        if (
            lastPacketMs <= 0L ||
            now - lastPacketMs > FEEDBACK_STALE_MS
        ) {
            stopRumble()
            return
        }

        val amplitude = mixGameRumbleAmplitude(
            largeMotor = largeMotor,
            smallMotor = smallMotor,
            strength = strength,
        )
        if (amplitude == 0) return

        val elapsedSinceEffect = now - lastEffectMs
        if (!force && elapsedSinceEffect < EFFECT_REFRESH_MS) return
        if (
            force &&
            lastEffectMs != 0L &&
            elapsedSinceEffect < MIN_FORCED_RESTART_MS
        ) {
            return
        }

        lastEffectMs = now
        lastAppliedAmplitude = amplitude

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(EFFECT_DURATION_MS, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(EFFECT_DURATION_MS)
        }
    }

    fun stopRumble() {
        largeMotor = 0
        smallMotor = 0
        lastPacketMs = 0L
        lastEffectMs = 0L
        lastAppliedAmplitude = 0
        vibrator?.cancel()
    }

    private fun closeSocketOnly() {
        socket?.close()
        socket = null
    }

    override fun close() {
        closeSocketOnly()
        stopRumble()
    }
}
