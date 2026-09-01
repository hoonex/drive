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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

enum class SteeringMode { MOTION, TILT, TOUCH }
enum class ReturnMode { SMOOTH, INSTANT, HOLD }

object ControllerButtonBits {
    const val SHIFT_UP = 1 shl 0
    const val SHIFT_DOWN = 1 shl 1
    const val HORN = 1 shl 2
    const val CAMERA = 1 shl 3
    const val RESET = 1 shl 4
}

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
    fun buttonMask(): Int {
        var buttons = 0
        if (shiftUp) buttons = buttons or ControllerButtonBits.SHIFT_UP
        if (shiftDown) buttons = buttons or ControllerButtonBits.SHIFT_DOWN
        if (horn) buttons = buttons or ControllerButtonBits.HORN
        if (camera) buttons = buttons or ControllerButtonBits.CAMERA
        if (reset) buttons = buttons or ControllerButtonBits.RESET
        return buttons
    }

    fun writeTo(buffer: ByteBuffer, sequence: Int, timestampMs: Long) {
        buffer.clear()
        buffer.putInt(sequence)
        buffer.putLong(timestampMs)
        buffer.putFloat(steering.coerceIn(-1f, 1f))
        buffer.putFloat(throttle.coerceIn(0f, 1f))
        buffer.putFloat(brake.coerceIn(0f, 1f))
        buffer.putFloat(clutch.coerceIn(0f, 1f))
        buffer.putFloat(handbrake.coerceIn(0f, 1f))
        buffer.putInt(buttonMask())
    }
}

/**
 * Mutable transport state for the high-frequency control path.
 *
 * Sensor and touch updates write primitive volatile fields instead of allocating a new
 * ControllerState for every event. UDP serializes these fields directly. Compose gets a
 * ControllerState snapshot only at the UI sampling rate.
 */
class ControllerStateStore(initial: ControllerState = ControllerState()) {
    @Volatile
    private var steering = initial.steering.coerceIn(-1f, 1f)

    @Volatile
    private var throttle = initial.throttle.coerceIn(0f, 1f)

    @Volatile
    private var brake = initial.brake.coerceIn(0f, 1f)

    @Volatile
    private var clutch = initial.clutch.coerceIn(0f, 1f)

    @Volatile
    private var handbrake = initial.handbrake.coerceIn(0f, 1f)

    private val buttons = AtomicInteger(initial.buttonMask())

    fun steeringValue(): Float = steering
    fun throttleValue(): Float = throttle
    fun brakeValue(): Float = brake
    fun clutchValue(): Float = clutch
    fun handbrakeValue(): Float = handbrake

    fun setSteering(value: Float): Boolean {
        val clamped = value.coerceIn(-1f, 1f)
        if (steering == clamped) return false
        steering = clamped
        return true
    }

    fun setThrottle(value: Float): Boolean = setUnitFloat(value, { throttle }, { throttle = it })
    fun setBrake(value: Float): Boolean = setUnitFloat(value, { brake }, { brake = it })
    fun setClutch(value: Float): Boolean = setUnitFloat(value, { clutch }, { clutch = it })
    fun setHandbrake(value: Float): Boolean = setUnitFloat(value, { handbrake }, { handbrake = it })

    private inline fun setUnitFloat(
        value: Float,
        current: () -> Float,
        update: (Float) -> Unit,
    ): Boolean {
        val clamped = value.coerceIn(0f, 1f)
        if (current() == clamped) return false
        update(clamped)
        return true
    }

    fun isButtonPressed(mask: Int): Boolean = buttons.get() and mask != 0

    fun setButton(mask: Int, pressed: Boolean): Boolean {
        while (true) {
            val old = buttons.get()
            val next = if (pressed) old or mask else old and mask.inv()
            if (old == next) return false
            if (buttons.compareAndSet(old, next)) return true
        }
    }

    fun reset() {
        steering = 0f
        throttle = 0f
        brake = 0f
        clutch = 0f
        handbrake = 0f
        buttons.set(0)
    }

    fun snapshot(): ControllerState {
        val mask = buttons.get()
        return ControllerState(
            steering = steering,
            throttle = throttle,
            brake = brake,
            clutch = clutch,
            handbrake = handbrake,
            shiftUp = mask and ControllerButtonBits.SHIFT_UP != 0,
            shiftDown = mask and ControllerButtonBits.SHIFT_DOWN != 0,
            horn = mask and ControllerButtonBits.HORN != 0,
            camera = mask and ControllerButtonBits.CAMERA != 0,
            reset = mask and ControllerButtonBits.RESET != 0,
        )
    }

    fun writeTo(buffer: ByteBuffer, sequence: Int, timestampMs: Long) {
        buffer.clear()
        buffer.putInt(sequence)
        buffer.putLong(timestampMs)
        buffer.putFloat(steering)
        buffer.putFloat(throttle)
        buffer.putFloat(brake)
        buffer.putFloat(clutch)
        buffer.putFloat(handbrake)
        buffer.putInt(buttons.get())
    }
}

class UdpClient(
    private val ip: String,
    private val port: Int,
    private val stateStore: ControllerStateStore,
    private val lowLatencyMode: Boolean,
) {
    companion object {
        private const val PACKET_SIZE = 36
        private const val ECHO_SIZE = 12
        private const val BASE_PERIOD_NS = 10_000_000L
        private const val FAST_PATH_MIN_NS = 6_000_000L
        private const val CONNECTION_TIMEOUT_NS = 750_000_000L
        private const val LATENCY_UI_PERIOD_NS = 75_000_000L
        private const val SENT_RING_SIZE = 512
    }

    private var socket: DatagramSocket? = null
    private var sequence = 0
    private val sendLock = ReentrantLock()
    private val sendCondition = sendLock.newCondition()
    private val sentLock = Any()
    private val sentSequences = IntArray(SENT_RING_SIZE)
    private val sentAtNanos = LongArray(SENT_RING_SIZE)
    private val sentValid = BooleanArray(SENT_RING_SIZE)

    @Volatile
    private var pendingImmediateSend = false

    @Volatile
    private var closing = false

    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)
    val maxPacketGapMicros = MutableStateFlow(0L)
    val lastError = MutableStateFlow<String?>(null)

    fun requestImmediateSend() {
        if (!lowLatencyMode || closing) return
        sendLock.withLock {
            pendingImmediateSend = true
            sendCondition.signal()
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        closing = false
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
            var maxGapNsInWindow = 0L

            fun publishTelemetryIfDue(nowNs: Long) {
                if (nowNs - windowStartNs < 1_000_000_000L) return
                val elapsedSeconds = (nowNs - windowStartNs) / 1_000_000_000.0
                packetRate.value = (packetsInWindow / elapsedSeconds).toInt()
                maxPacketGapMicros.value = maxGapNsInWindow / 1_000L
                packetsInWindow = 0
                maxGapNsInWindow = 0L
                windowStartNs = nowNs
            }

            try {
                while (currentCoroutineContext().isActive && !closing) {
                    val nowNs = SystemClock.elapsedRealtimeNanos()
                    publishTelemetryIfDue(nowNs)

                    val nextFastPathNs = if (
                        lowLatencyMode && pendingImmediateSend && lastSendNs > 0L
                    ) {
                        lastSendNs + FAST_PATH_MIN_NS
                    } else {
                        Long.MAX_VALUE
                    }
                    val nextDueNs = min(nextHeartbeatNs, nextFastPathNs)
                    val waitNs = nextDueNs - nowNs

                    if (waitNs > 0L) {
                        sendLock.withLock {
                            if (!closing) {
                                sendCondition.awaitNanos(waitNs)
                            }
                        }
                        continue
                    }

                    val sendNowNs = SystemClock.elapsedRealtimeNanos()
                    val shouldSend = sendLock.withLock {
                        val heartbeatDue = sendNowNs >= nextHeartbeatNs
                        val fastPathDue = lowLatencyMode &&
                            pendingImmediateSend &&
                            (lastSendNs == 0L || sendNowNs - lastSendNs >= FAST_PATH_MIN_NS)

                        if (heartbeatDue || fastPathDue) {
                            // This packet contains the latest state available now. A newer input arriving
                            // after this point will set the flag again and wake the sender for the next slot.
                            pendingImmediateSend = false
                            true
                        } else {
                            false
                        }
                    }

                    if (!shouldSend) continue

                    if (sendState(datagramSocket, packetBuffer, outboundPacket, sendNowNs)) {
                        if (lastSendNs > 0L) {
                            maxGapNsInWindow = max(maxGapNsInWindow, sendNowNs - lastSendNs)
                        }
                        packetsInWindow++
                        lastSendNs = sendNowNs
                        nextHeartbeatNs = sendNowNs + BASE_PERIOD_NS
                    }
                    publishTelemetryIfDue(sendNowNs)
                }
            } catch (e: CancellationException) {
                throw e
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
        stateStore.writeTo(packetBuffer, currentSequence, System.currentTimeMillis())

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

        while (currentCoroutineContext().isActive) {
            try {
                echoPacket.length = echoBytes.size
                datagramSocket.receive(echoPacket)
                if (echoPacket.length < ECHO_SIZE) continue

                echoReader.clear()
                val echoedSequence = echoReader.int
                echoReader.long

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
        closing = true
        sendLock.withLock {
            pendingImmediateSend = false
            sendCondition.signalAll()
        }
        socket?.close()
        socket = null
        isConnected.value = false
    }
}

class SensorHandler(context: Context) : SensorEventListener, AutoCloseable {
    companion object {
        // Transport peaks around 166 Hz, so sampling above 200 Hz only adds wakeups/heat.
        private const val MOTION_LOW_LATENCY_US = 5_000
        private const val TILT_LOW_LATENCY_US = 10_000
        private const val BALANCED_SENSOR_US = 20_000
    }

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

    @Volatile
    var eventRateHz: Int = 0
        private set

    var onMotionAngleChanged: ((Float) -> Unit)? = null
    var onTiltAngleChanged: ((Float) -> Unit)? = null

    private val rotationMatrix = FloatArray(9)
    private val baseMatrix = FloatArray(9)
    private var hasMotionBase = false
    private var previousRawAngle = 0f
    private var accumulatedAngle = 0f
    private var tiltZeroAngle = 0f
    private var eventWindowStartNs = 0L
    private var eventsInWindow = 0

    @Volatile
    private var motionResetRequested = true

    @Volatile
    private var tiltResetRequested = true

    fun start(mode: SteeringMode, lowLatencyMode: Boolean) {
        stop()
        eventWindowStartNs = SystemClock.elapsedRealtimeNanos()
        eventsInWindow = 0
        eventRateHz = 0

        val samplingPeriodUs = when {
            !lowLatencyMode -> BALANCED_SENSOR_US
            mode == SteeringMode.MOTION -> MOTION_LOW_LATENCY_US
            mode == SteeringMode.TILT -> TILT_LOW_LATENCY_US
            else -> BALANCED_SENSOR_US
        }

        when (mode) {
            SteeringMode.MOTION -> {
                motionResetRequested = true
                rotationSensor?.let {
                    sensorManager.registerListener(
                        this,
                        it,
                        samplingPeriodUs,
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
                        samplingPeriodUs,
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
        eventRateHz = 0
    }

    fun calibrate(mode: SteeringMode) {
        when (mode) {
            SteeringMode.MOTION -> motionResetRequested = true
            SteeringMode.TILT -> tiltResetRequested = true
            SteeringMode.TOUCH -> Unit
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        updateEventRate()
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR,
            -> handleRotation(event)

            Sensor.TYPE_ACCELEROMETER -> handleTilt(event)
        }
    }

    private fun updateEventRate() {
        val nowNs = SystemClock.elapsedRealtimeNanos()
        if (eventWindowStartNs == 0L) eventWindowStartNs = nowNs
        eventsInWindow++
        val elapsedNs = nowNs - eventWindowStartNs
        if (elapsedNs >= 1_000_000_000L) {
            eventRateHz = ((eventsInWindow * 1_000_000_000L) / elapsedNs).toInt()
            eventsInWindow = 0
            eventWindowStartNs = nowNs
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
