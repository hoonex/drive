package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.asin
import kotlin.math.atan2

enum class SteeringMode { MOTION, TILT, TOUCH }
enum class ReturnMode { SMOOTH, INSTANT, HOLD }

class HapticManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    }
    
    private var lastShiftTime = 0L

    fun shiftUp() {
        val now = System.currentTimeMillis()
        if (now - lastShiftTime < 50) return
        lastShiftTime = now
        vibrate(30, 255)
    }
    
    fun shiftDown() {
        val now = System.currentTimeMillis()
        if (now - lastShiftTime < 50) return
        lastShiftTime = now
        vibrate(30, 255)
    }
    
    fun handbrakePress() {
        vibrate(50, 150)
    }
    
    fun handbrakeRelease() {
        vibrate(20, 100)
    }
    
    fun horn() {
        vibrate(10, 50)
    }
    
    fun recenter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), intArrayOf(0, 200, 0, 200), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 30, 50, 30), -1)
        }
    }
    
    fun modeChange() {
        vibrate(15, 80)
    }
    
    private fun vibrate(duration: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("pc_wheel_settings", Context.MODE_PRIVATE)

    var ip: String
        get() = prefs.getString("ip", "192.168.1.100") ?: "192.168.1.100"
        set(value) = prefs.edit().putString("ip", value).apply()

    var port: Int
        get() = prefs.getInt("port", 4444)
        set(value) = prefs.edit().putInt("port", value).apply()

    var steeringMode: SteeringMode
        get() = SteeringMode.valueOf(prefs.getString("mode", SteeringMode.MOTION.name) ?: SteeringMode.MOTION.name)
        set(value) = prefs.edit().putString("mode", value.name).apply()

    var steeringRange: Int
        get() = prefs.getInt("range", 900)
        set(value) = prefs.edit().putInt("range", value).apply()

    var tiltSensitivity: Float
        get() = prefs.getFloat("tiltSens", 1.0f)
        set(value) = prefs.edit().putFloat("tiltSens", value).apply()

    var touchReturnMode: ReturnMode
        get() = ReturnMode.valueOf(prefs.getString("touchReturn", ReturnMode.SMOOTH.name) ?: ReturnMode.SMOOTH.name)
        set(value) = prefs.edit().putString("touchReturn", value.name).apply()
}

data class ControllerState(
    var steering: Float = 0f, // -1 to 1
    var throttle: Float = 0f, // 0 to 1
    var brake: Float = 0f, // 0 to 1
    var clutch: Float = 0f, // 0 to 1
    var handbrake: Float = 0f, // 0 to 1
    var shiftUp: Boolean = false,
    var shiftDown: Boolean = false,
    var horn: Boolean = false,
    var camera: Boolean = false,
    var reset: Boolean = false
) {
    fun toBytes(sequence: Int): ByteArray {
        val buffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(sequence)
        buffer.putLong(System.currentTimeMillis())
        buffer.putFloat(steering)
        buffer.putFloat(throttle)
        buffer.putFloat(brake)
        buffer.putFloat(clutch)
        buffer.putFloat(handbrake)
        var buttons = 0
        if (shiftUp) buttons = buttons or (1 shl 0)
        if (shiftDown) buttons = buttons or (1 shl 1)
        if (horn) buttons = buttons or (1 shl 2)
        if (camera) buttons = buttons or (1 shl 3)
        if (reset) buttons = buttons or (1 shl 4)
        buffer.putInt(buttons)
        return buffer.array()
    }
}

class UdpClient(private val ip: String, private val port: Int) {
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var seq = 0

    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)

    suspend fun start(stateFlow: MutableStateFlow<ControllerState>) = withContext(Dispatchers.IO) {
        try {
            socket = DatagramSocket().apply { soTimeout = 1000 }
            address = InetAddress.getByName(ip)
        } catch (e: Exception) {
            return@withContext
        }

        var packetsSentInSec = 0
        var lastTime = System.currentTimeMillis()

        // Latency receiver coroutine
        launch {
            val buffer = ByteArray(12)
            val packet = DatagramPacket(buffer, buffer.size)
            while (isActive) {
                try {
                    socket?.receive(packet)
                    val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                    val recvSeq = bb.getInt()
                    val ts = bb.getLong()
                    latency.value = System.currentTimeMillis() - ts
                    isConnected.value = true
                } catch (e: SocketTimeoutException) {
                    isConnected.value = false
                } catch (e: Exception) {
                    delay(100)
                }
            }
        }

        while (isActive) {
            val state = stateFlow.value
            val buffer = state.toBytes(seq++)
            val packet = DatagramPacket(buffer, buffer.size, address, port)
            try {
                socket?.send(packet)
                packetsSentInSec++
            } catch (e: Exception) {
                // Ignore send errors to maintain loop speed
            }

            val now = System.currentTimeMillis()
            if (now - lastTime >= 1000) {
                packetRate.value = packetsSentInSec
                packetsSentInSec = 0
                lastTime = now
            }
            delay(10) // Approx 100Hz
        }
    }

    fun close() {
        socket?.close()
    }
}

class SensorHandler(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val hasRotationSensor = rotationSensor != null
    val hasAccelSensor = accelSensor != null

    var onMotionAngleChanged: ((Float) -> Unit)? = null
    var onTiltAngleChanged: ((Float) -> Unit)? = null

    private var baseMatrix: FloatArray? = null
    private var previousRawAngle = 0f
    private var accumulatedAngle = 0f

    fun start() {
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun calibrate() {
        baseMatrix = null
        accumulatedAngle = 0f
        previousRawAngle = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val r = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(r, event.values)
            
            val currentBase = baseMatrix
            if (currentBase == null) {
                baseMatrix = r.clone()
                previousRawAngle = 0f
                return
            }

            // Project current rotation relative to base rotation onto the XY plane.
            // R_rel = R_base^T * R_current. We need column 0 of R_rel (which represents the new local X axis).
            val r00 = currentBase[0] * r[0] + currentBase[3] * r[3] + currentBase[6] * r[6]
            val r10 = currentBase[1] * r[0] + currentBase[4] * r[3] + currentBase[7] * r[6]
            
            val rawAngle = atan2(r10.toDouble(), r00.toDouble()).toFloat()
            var delta = rawAngle - previousRawAngle
            
            // Unwrap angle
            if (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
            if (delta < -Math.PI) delta += (2 * Math.PI).toFloat()
            
            accumulatedAngle += delta
            previousRawAngle = rawAngle

            onMotionAngleChanged?.invoke(Math.toDegrees(accumulatedAngle.toDouble()).toFloat())
        }
        else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // In landscape, left/right tilt affects the Y axis (values[1])
            val y = event.values[1]
            val tiltAngle = Math.toDegrees(asin((y / 9.8f).coerceIn(-1f, 1f).toDouble())).toFloat()
            onTiltAngleChanged?.invoke(tiltAngle)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
