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
import kotlin.math.max

/** Receives XInput rumble relayed by PC Wheel Receiver on a path separate from steering input. */
class GameFeedbackClient(context: Context) : AutoCloseable {
    companion object {
        const val FEEDBACK_PORT = 26762
        private const val PACKET_SIZE = 8
        private const val REFRESH_MS = 30L
        private const val PULSE_MS = 45L
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

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

    private var lastPulseMs = 0L

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
                soTimeout = REFRESH_MS.toInt()
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
                    if (largeMotor == 0 && smallMotor == 0) {
                        stopRumble()
                    } else {
                        refreshRumble(force = true)
                    }
                } catch (_: SocketTimeoutException) {
                    // XInput feedback is change-driven, so refresh short pulses while the
                    // last requested motor state remains non-zero.
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
        if (!enabled || vibrator?.hasVibrator() != true) return
        val large = largeMotor / 255f
        val small = smallMotor / 255f
        if (large <= 0.001f && small <= 0.001f) return

        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPulseMs < REFRESH_MS - 3L) return
        lastPulseMs = now

        // A phone normally exposes one haptic actuator while XInput has two motors.
        // Preserve strong low-frequency impacts while retaining high-frequency texture.
        val mixed = max(large * 0.90f, small * 0.72f)
        val amplitude = (mixed * strength.coerceIn(0.25f, 1.5f) * 255f)
            .toInt()
            .coerceIn(1, 255)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(PULSE_MS, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(PULSE_MS)
        }
    }

    fun stopRumble() {
        largeMotor = 0
        smallMotor = 0
        lastPulseMs = 0L
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
