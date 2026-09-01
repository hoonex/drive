package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    val settings = SettingsRepository(application)
    val uiSettings = ControllerUiPreferences(application)
    val sensorHandler = SensorHandler(application)
    val haptics = HapticManager(application)

    private val liveState = AtomicReference(ControllerState())
    private val _controllerState = MutableStateFlow(liveState.get())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()

    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)
    val connectionError = MutableStateFlow<String?>(null)

    private var udpClient: UdpClient? = null
    private var networkJob: Job? = null
    private var uiSamplerJob: Job? = null
    private val telemetryJobs = mutableListOf<Job>()
    private var returnJob: Job? = null

    @Volatile
    private var activeSteeringMode = settings.steeringMode

    @Volatile
    private var activeSteeringRange = settings.steeringRange

    @Volatile
    private var activeTiltSensitivity = settings.tiltSensitivity

    @Volatile
    var currentSteeringAngleDeg = 0f
        private set

    init {
        haptics.enabled = settings.hapticsEnabled

        sensorHandler.onMotionAngleChanged = { angle ->
            if (activeSteeringMode == SteeringMode.MOTION) {
                // Physical clockwise rotation should be positive/right steering.
                updateSteeringFromAngle(-angle)
            }
        }
        sensorHandler.onTiltAngleChanged = { angle ->
            if (activeSteeringMode == SteeringMode.TILT) {
                updateSteeringFromAngle(angle * activeTiltSensitivity)
            }
        }
    }

    fun startController() {
        stopController(resetInputs = false)

        activeSteeringMode = settings.steeringMode
        activeSteeringRange = settings.steeringRange
        activeTiltSensitivity = settings.tiltSensitivity
        haptics.enabled = settings.hapticsEnabled
        connectionError.value = null

        sensorHandler.start(activeSteeringMode, settings.lowLatencyMode)

        val client = UdpClient(
            ip = settings.ip,
            port = settings.port,
            stateProvider = { liveState.get() },
            lowLatencyMode = settings.lowLatencyMode,
        )
        udpClient = client

        telemetryJobs += viewModelScope.launch {
            client.isConnected.collect { isConnected.value = it }
        }
        telemetryJobs += viewModelScope.launch {
            client.latency.collect { latency.value = it }
        }
        telemetryJobs += viewModelScope.launch {
            client.packetRate.collect { packetRate.value = it }
        }
        telemetryJobs += viewModelScope.launch {
            client.lastError.collect { connectionError.value = it }
        }

        // Sensor/network state remains hot while Compose receives display snapshots at ~30 Hz.
        uiSamplerJob = viewModelScope.launch {
            while (true) {
                val latest = liveState.get()
                if (_controllerState.value != latest) {
                    _controllerState.value = latest
                }
                delay(33)
            }
        }

        networkJob = viewModelScope.launch(Dispatchers.IO) {
            client.start()
        }
    }

    fun stopController() {
        stopController(resetInputs = true)
    }

    private fun stopController(resetInputs: Boolean) {
        sensorHandler.stop()
        returnJob?.cancel()
        returnJob = null
        uiSamplerJob?.cancel()
        uiSamplerJob = null
        networkJob?.cancel()
        networkJob = null
        telemetryJobs.forEach { it.cancel() }
        telemetryJobs.clear()
        udpClient?.close()
        udpClient = null

        isConnected.value = false
        latency.value = 0L
        packetRate.value = 0
        connectionError.value = null

        if (resetInputs) {
            currentSteeringAngleDeg = 0f
            val neutral = ControllerState()
            liveState.set(neutral)
            _controllerState.value = neutral
        }
    }

    fun calibrate() {
        sensorHandler.calibrate(activeSteeringMode)
        updateSteeringFromAngle(0f)
        if (haptics.enabled) haptics.recenter()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settings.hapticsEnabled = enabled
        haptics.enabled = enabled
        if (enabled) haptics.modeChange()
    }

    fun updateAnalog(type: AnalogInput, value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        val oldState = liveState.get()

        if (type == AnalogInput.HANDBRAKE && haptics.enabled) {
            if (oldState.handbrake <= 0.001f && clamped > 0.001f) {
                haptics.handbrakePress()
            } else if (oldState.handbrake > 0.001f && clamped <= 0.001f) {
                haptics.handbrakeRelease()
            }
        }

        mutateLiveState { state ->
            when (type) {
                AnalogInput.THROTTLE -> state.copy(throttle = clamped)
                AnalogInput.BRAKE -> state.copy(brake = clamped)
                AnalogInput.CLUTCH -> state.copy(clutch = clamped)
                AnalogInput.HANDBRAKE -> state.copy(handbrake = clamped)
            }
        }
    }

    fun updateButton(type: ButtonInput, pressed: Boolean) {
        val oldState = liveState.get()
        if (pressed && haptics.enabled) {
            when (type) {
                ButtonInput.SHIFT_UP -> if (!oldState.shiftUp) haptics.shiftUp()
                ButtonInput.SHIFT_DOWN -> if (!oldState.shiftDown) haptics.shiftDown()
                ButtonInput.HORN -> if (!oldState.horn) haptics.horn()
                else -> Unit
            }
        }

        mutateLiveState { state ->
            when (type) {
                ButtonInput.SHIFT_UP -> state.copy(shiftUp = pressed)
                ButtonInput.SHIFT_DOWN -> state.copy(shiftDown = pressed)
                ButtonInput.HORN -> state.copy(horn = pressed)
                ButtonInput.CAMERA -> state.copy(camera = pressed)
                ButtonInput.RESET -> state.copy(reset = pressed)
            }
        }
    }

    fun handleTouchWheelDelta(delta: Float) {
        returnJob?.cancel()
        returnJob = null
        updateSteeringFromAngle(currentSteeringAngleDeg + delta)
    }

    fun handleTouchWheelRelease() {
        when (settings.touchReturnMode) {
            ReturnMode.INSTANT -> updateSteeringFromAngle(0f)
            ReturnMode.HOLD -> Unit
            ReturnMode.SMOOTH -> {
                returnJob?.cancel()
                returnJob = viewModelScope.launch {
                    var angle = currentSteeringAngleDeg
                    while (abs(angle) > 0.6f) {
                        angle *= 0.78f
                        updateSteeringFromAngle(angle)
                        delay(16)
                    }
                    updateSteeringFromAngle(0f)
                }
            }
        }
    }

    private fun updateSteeringFromAngle(angle: Float) {
        currentSteeringAngleDeg = angle
        val halfRange = (activeSteeringRange.coerceAtLeast(180) / 2f).coerceAtLeast(1f)
        val normalized = (angle / halfRange).coerceIn(-1f, 1f)
        mutateLiveState { it.copy(steering = normalized) }
    }

    private inline fun mutateLiveState(transform: (ControllerState) -> ControllerState) {
        while (true) {
            val oldState = liveState.get()
            val newState = transform(oldState)
            if (newState == oldState) return
            if (liveState.compareAndSet(oldState, newState)) {
                udpClient?.requestImmediateSend()
                return
            }
        }
    }

    override fun onCleared() {
        stopController(resetInputs = true)
        sensorHandler.close()
        super.onCleared()
    }
}

enum class AnalogInput { THROTTLE, BRAKE, CLUTCH, HANDBRAKE }
enum class ButtonInput { SHIFT_UP, SHIFT_DOWN, HORN, CAMERA, RESET }
