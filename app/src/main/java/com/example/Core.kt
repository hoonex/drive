package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

enum class SteeringMode { MOTION, TILT, TOUCH }
enum class ReturnMode { SMOOTH, INSTANT, HOLD }

class HapticManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var enabled: Boolean = true

    private var lastShiftTimeMs = 0L

    fun shiftUp() = shiftPulse()
    fun shiftDown() = shiftPulse()
    fun handbrakePress() = vibrate(36, 145)
    fun handbrakeRelease() = vibrate(18, 90)
    fun horn() = vibrate(10, 45)
    fun modeChange() = vibrate(14, 80)

    fun recenter() {
        if (!enabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 24, 42, 24),
                    intArrayOf(0, 180, 0, 180),
                    -1,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 24, 42, 24), -1)
        }
    }

    private fun shiftPulse() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShiftTimeMs < 45L) return
        lastShiftTimeMs = now
        vibrate(26, 220)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (!enabled || vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    amplitude.coerceIn(1, 255),
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pc_wheel_settings", Context.MODE_PRIVATE)

    var ip: String
        get() = prefs.getString("ip", "192.168.1.100") ?: "192.168.1.100"
        set(value) = prefs.edit().putString("ip", value.trim()).apply()

    var port: Int
        get() = prefs.getInt("port", 26760)
        set(value) = prefs.edit().putInt("port", value.coerceIn(1, 65535)).apply()

    var steeringMode: SteeringMode
        get() = runCatching {
            SteeringMode.valueOf(prefs.getString("mode", SteeringMode.MOTION.name) ?: SteeringMode.MOTION.name)
        }.getOrDefault(SteeringMode.MOTION)
        set(value) = prefs.edit().putString("mode", value.name).apply()

    var steeringRange: Int
        get() = prefs.getInt("range", 900).coerceIn(180, 1080)
        set(value) = prefs.edit().putInt("range", value.coerceIn(180, 1080)).apply()

    var tiltSensitivity: Float
        get() = prefs.getFloat("tiltSens", 1.0f).coerceIn(0.5f, 3.0f)
        set(value) = prefs.edit().putFloat("tiltSens", value.coerceIn(0.5f, 3.0f)).apply()

    var touchReturnMode: ReturnMode
        get() = runCatching {
            ReturnMode.valueOf(prefs.getString("touchReturn", ReturnMode.SMOOTH.name) ?: ReturnMode.SMOOTH.name)
        }.getOrDefault(ReturnMode.SMOOTH)
        set(value) = prefs.edit().putString("touchReturn", value.name).apply()

    var lowLatencyMode: Boolean
        get() = prefs.getBoolean("lowLatencyMode", true)
        set(value) = prefs.edit().putBoolean("lowLatencyMode", value).apply()

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("hapticsEnabled", true)
        set(value) = prefs.edit().putBoolean("hapticsEnabled", value).apply()
}

data class ControllerState(
    val steering: Float = 0f,
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val clutch: Float = 0f,
    val handbrake: Float = 0f,
    val shiftUp: Boolean = false,
    val shiftDown: Boolean = false,
    val horn: Boolean = false,
    val camera: Boolean = false,
    val reset: Boolean = false,
) {
    fun writeTo(buffer: ByteBuffer, sequence: Int, timestampMs: Long) {
        buffer.clear()
        buffer.putInt(sequence)
        buffer.putLong(timestampMs)
        buffer.putFloat(steering.coerceIn(-1f, 1f))
        buffer.putFloat(throttle.coerceIn(0f, 1f))
        buffer.putFloat(brake.coerceIn(0f, 1f))
        buffer.putFloat(clutch.coerceIn(0f, 1f))
        buffer.putFloat(handbrake.coerceIn(0f, 1f))

        var buttons = 0
        if (shiftUp) buttons = buttons or (1 shl 0)
        if (shiftDown) buttons = buttons or (1 shl 1)
        if (horn) buttons = buttons or (1 shl 2)
        if (camera) buttons = buttons or (1 shl 3)
        if (reset) buttons = buttons or (1 shl 4)
        buffer.putInt(buttons)
    }
}

class UdpClient(
    private val ip: String,
    private val port: Int,
    private val stateProvider: () -> ControllerState,
    private val lowLatencyMode: Boolean,
) {
    companion object {
        private const val PACKET_SIZE = 36
        private const val ECHO_SIZE = 12
        private const val BASE_PERIOD_NS = 10_000_000L // 100 Hz heartbeat
        private const val FAST_PATH_MIN_NS = 6_000_000L // <= ~166 Hz while input is actively changing
        private const val CONNECTION_TIMEOUT_NS = 750_000_000L
        private const val LATENCY_UI_PERIOD_NS = 75_000_000L
        private const val SENT_RING_SIZE = 512
    }

    private var socket: DatagramSocket? = null
    private var sequence = 0
    private val sendSignal = Channel<Unit>(Channel.CONFLATED)
    private val sentLock = Any()
    private val sentSequences = IntArray(SENT_RING_SIZE)
    private val sentAtNanos = LongArray(SENT_RING_SIZE)
    private val sentValid = BooleanArray(SENT_RING_SIZE)

    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)
    val lastError = MutableStateFlow<String?>(null)

    fun requestImmediateSend() {
        sendSignal.trySend(Unit)
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        val resolvedAddress = try {
            InetAddress.getByName(ip)
        } catch (e: Exception) {
            lastError.value = "Invalid PC address: ${e.message ?: ip}"
            return@withContext
        }

        val datagramSocket = try {
            DatagramSocket().apply {
                soTimeout = 250
                receiveBufferSize = 64 * 1024
                sendBufferSize = 64 * 1024
            }
        } catch (e: Exception) {
            lastError.value = "Unable to open UDP socket: ${e.message ?: "unknown error"}"
            return@withContext
        }

        socket = datagramSocket
        isConnected.value = false
        lastError.value = null

        val packetBuffer = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        val outboundPacket = DatagramPacket(packetBuffer.array(), PACKET_SIZE, resolvedAddress, port)

        supervisorScope {
            val receiverJob = launch(Dispatchers.IO) {
                receiveEchoLoop(datagramSocket)
            }

            var lastSendNs = 0L
            var nextHeartbeatNs = SystemClock.elapsedRealtimeNanos()
            var windowStartNs = nextHeartbeatNs
            var packetsInWindow = 0

            try {
                while (isActive) {
                    val nowNs = SystemClock.elapsedRealtimeNanos()
                    val waitNs = nextHeartbeatNs - nowNs

                    if (waitNs > 0L) {
                        val waitMs = ((waitNs + 999_999L) / 1_000_000L).coerceAtLeast(1L)
                        val inputChanged = withTimeoutOrNull(waitMs) {
                            sendSignal.receive()
                            true
                        } ?: false

                        if (inputChanged) {
                            if (lowLatencyMode) {
                                val signalNowNs = SystemClock.elapsedRealtimeNanos()
                                if (lastSendNs == 0L || signalNowNs - lastSendNs >= FAST_PATH_MIN_NS) {
                                    if (sendState(datagramSocket, packetBuffer, outboundPacket, signalNowNs)) {
                                        packetsInWindow++
                                        lastSendNs = signalNowNs
                                        nextHeartbeatNs = signalNowNs + BASE_PERIOD_NS
                                    }
                                }
                            }
                            continue
                        }
                    }

                    val sendNowNs = SystemClock.elapsedRealtimeNanos()
                    if (sendState(datagramSocket, packetBuffer, outboundPacket, sendNowNs)) {
                        packetsInWindow++
                        lastSendNs = sendNowNs
                    }
                    nextHeartbeatNs = sendNowNs + BASE_PERIOD_NS

                    val rateNowNs = SystemClock.elapsedRealtimeNanos()
                    if (rateNowNs - windowStartNs >= 1_000_000_000L) {
                        val elapsedSeconds = (rateNowNs - windowStartNs) / 1_000_000_000.0
                        packetRate.value = (packetsInWindow / elapsedSeconds).toInt()
                        packetsInWindow = 0
                        windowStartNs = rateNowNs
                    }
                }
            } catch (_: CancellationException) {
                throw CancellationException()
            } finally {
                receiverJob.cancel()
                datagramSocket.close()
                socket = null
                isConnected.value = false
            }
        }
    }

    private fun sendState(
        datagramSocket: DatagramSocket,
        packetBuffer: ByteBuffer,
        packet: DatagramPacket,
        monotonicNowNs: Long,
    ): Boolean {
        val currentSequence = sequence++
        stateProvider().writeTo(packetBuffer, currentSequence, System.currentTimeMillis())

        return try {
            datagramSocket.send(packet)
            rememberSend(currentSequence, monotonicNowNs)
            true
        } catch (e: SocketException) {
            if (!datagramSocket.isClosed) {
                lastError.value = "UDP send failed: ${e.message ?: "socket error"}"
            }
            false
        } catch (e: Exception) {
            lastError.value = "UDP send failed: ${e.message ?: "unknown error"}"
            false
        }
    }

    private suspend fun receiveEchoLoop(datagramSocket: DatagramSocket) {
        val echoBytes = ByteArray(ECHO_SIZE)
        val echoPacket = DatagramPacket(echoBytes, echoBytes.size)
        val echoReader = ByteBuffer.wrap(echoBytes).order(ByteOrder.LITTLE_ENDIAN)
        var lastEchoNs = 0L
        var lastLatencyPublishNs = 0L

        while (isActive) {
            try {
                echoPacket.length = echoBytes.size
                datagramSocket.receive(echoPacket)
                if (echoPacket.length < ECHO_SIZE) continue

                echoReader.clear()
                val echoedSequence = echoReader.int
                echoReader.long // Keep consuming the on-wire timestamp; RTT uses a monotonic local clock.

                val nowNs = SystemClock.elapsedRealtimeNanos()
                lastEchoNs = nowNs
                if (!isConnected.value) isConnected.value = true
                lastError.value = null

                val sentNs = lookupSend(echoedSequence)
                if (sentNs > 0L && nowNs - lastLatencyPublishNs >= LATENCY_UI_PERIOD_NS) {
                    latency.value = ((nowNs - sentNs) / 1_000_000L).coerceAtLeast(0L)
                    lastLatencyPublishNs = nowNs
                }
            } catch (_: SocketTimeoutException) {
                val nowNs = SystemClock.elapsedRealtimeNanos()
                if (lastEchoNs == 0L || nowNs - lastEchoNs > CONNECTION_TIMEOUT_NS) {
                    isConnected.value = false
                }
            } catch (e: SocketException) {
                if (datagramSocket.isClosed) return
                lastError.value = "UDP receive failed: ${e.message ?: "socket error"}"
                delay(20)
            } catch (e: Exception) {
                lastError.value = "UDP receive failed: ${e.message ?: "unknown error"}"
                delay(20)
            }
        }
    }

    private fun rememberSend(sequence: Int, sentNs: Long) {
        val index = sequence and (SENT_RING_SIZE - 1)
        synchronized(sentLock) {
            sentSequences[index] = sequence
            sentAtNanos[index] = sentNs
            sentValid[index] = true
        }
    }

    private fun lookupSend(sequence: Int): Long {
        val index = sequence and (SENT_RING_SIZE - 1)
        return synchronized(sentLock) {
            if (sentValid[index] && sentSequences[index] == sequence) sentAtNanos[index] else 0L
        }
    }

    fun close() {
        socket?.close()
        socket = null
        isConnected.value = false
    }
}

class SensorHandler(context: Context) : SensorEventListener, AutoCloseable {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorThread = HandlerThread(
        "pc-wheel-sensors",
        Process.THREAD_PRIORITY_MORE_FAVORABLE,
    ).apply { start() }
    private val sensorThreadHandler = Handler(sensorThread.looper)

    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val hasRotationSensor = rotationSensor != null
    val hasAccelSensor = accelSensor != null

    var onMotionAngleChanged: ((Float) -> Unit)? = null
    var onTiltAngleChanged: ((Float) -> Unit)? = null

    private val rotationMatrix = FloatArray(9)
    private val baseMatrix = FloatArray(9)
    private var hasMotionBase = false
    private var previousRawAngle = 0f
    private var accumulatedAngle = 0f
    private var tiltZeroAngle = 0f

    @Volatile
    private var motionResetRequested = true

    @Volatile
    private var tiltResetRequested = true

    fun start(mode: SteeringMode, lowLatencyMode: Boolean) {
        stop()
        val samplingPeriod = if (lowLatencyMode) {
            SensorManager.SENSOR_DELAY_FASTEST
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }

        when (mode) {
            SteeringMode.MOTION -> {
                motionResetRequested = true
                rotationSensor?.let {
                    sensorManager.registerListener(
                        this,
                        it,
                        samplingPeriod,
                        0,
                        sensorThreadHandler,
                    )
                }
            }
            SteeringMode.TILT -> {
                tiltResetRequested = true
                accelSensor?.let {
                    sensorManager.registerListener(
                        this,
                        it,
                        samplingPeriod,
                        0,
                        sensorThreadHandler,
                    )
                }
            }
            SteeringMode.TOUCH -> Unit
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun calibrate(mode: SteeringMode) {
        when (mode) {
            SteeringMode.MOTION -> motionResetRequested = true
            SteeringMode.TILT -> tiltResetRequested = true
            SteeringMode.TOUCH -> Unit
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR,
            -> handleRotation(event)

            Sensor.TYPE_ACCELEROMETER -> handleTilt(event)
        }
    }

    private fun handleRotation(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        if (motionResetRequested || !hasMotionBase) {
            rotationMatrix.copyInto(baseMatrix)
            previousRawAngle = 0f
            accumulatedAngle = 0f
            hasMotionBase = true
            motionResetRequested = false
            onMotionAngleChanged?.invoke(0f)
            return
        }

        // R_rel = R_base^T * R_current. Project its local X axis onto the XY plane.
        val r00 = baseMatrix[0] * rotationMatrix[0] +
            baseMatrix[3] * rotationMatrix[3] +
            baseMatrix[6] * rotationMatrix[6]
        val r10 = baseMatrix[1] * rotationMatrix[0] +
            baseMatrix[4] * rotationMatrix[3] +
            baseMatrix[7] * rotationMatrix[6]

        val rawAngle = atan2(r10, r00)
        var delta = rawAngle - previousRawAngle
        val pi = PI.toFloat()
        val twoPi = (2.0 * PI).toFloat()
        if (delta > pi) delta -= twoPi
        if (delta < -pi) delta += twoPi

        accumulatedAngle += delta
        previousRawAngle = rawAngle
        onMotionAngleChanged?.invoke(Math.toDegrees(accumulatedAngle.toDouble()).toFloat())
    }

    private fun handleTilt(event: SensorEvent) {
        val y = event.values[1]
        val rawTiltAngle = Math.toDegrees(
            asin((y / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f).toDouble()),
        ).toFloat()

        if (tiltResetRequested) {
            tiltZeroAngle = rawTiltAngle
            tiltResetRequested = false
            onTiltAngleChanged?.invoke(0f)
            return
        }

        onTiltAngleChanged?.invoke(rawTiltAngle - tiltZeroAngle)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun close() {
        stop()
        sensorThread.quitSafely()
    }
}
