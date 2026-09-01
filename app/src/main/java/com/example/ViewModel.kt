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
import kotlin.math.abs

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    val settings = SettingsRepository(application)
    val uiSettings = ControllerUiPreferences(application)
    val sensorHandler = SensorHandler(application)
    val haptics = HapticManager(application)

    private val liveState = ControllerStateStore()
    private val _controllerState = MutableStateFlow(liveState.snapshot())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()

    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)
    val maxPacketGapMicros = MutableStateFlow(0L)
    val sensorRateHz = MutableStateFlow(0)
    val connectionError = MutableStateFlow<String?>(null)

    private var udpClient: UdpClient? = null
    private var networkJob: Job? = null
    private var uiSamplerJob: Job? = null
    private val telemetryJobs = mutableListOf<Job>()
    private var returnJob: Job? = null

    @Volatile
    private var controllerRequested = false

    @Volatile
    private var controllerRunning = false

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
        controllerRequested = true
        startControllerInternal()
    }

    fun stopController() {
        controllerRequested = false
        stopControllerInternal(resetInputs = true)
    }

    /** Stop sensor/network work while the Activity is not visible without forgetting the controller screen. */
    fun pauseControllerForBackground() {
        if (!controllerRunning) return
        stopControllerInternal(resetInputs = true)
    }

    /** Restore the controller automatically after returning from Home, lock screen or another Activity. */
    fun resumeControllerIfRequested() {
        if (controllerRequested && !controllerRunning) {
            startControllerInternal()
        }
    }

    private fun startControllerInternal() {
        if (controllerRunning) return
        stopControllerInternal(resetInputs = false)
        controllerRunning = true

        activeSteeringMode = settings.steeringMode
        activeSteeringRange = settings.steeringRange
        activeTiltSensitivity = settings.tiltSensitivity
        haptics.enabled = settings.hapticsEnabled
        connectionError.value = null

        sensorHandler.start(activeSteeringMode, settings.lowLatencyMode)

        val client = UdpClient(
            ip = settings.ip,
            port = settings.port,
            stateStore = liveState,
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
            client.maxPacketGapMicros.collect { maxPacketGapMicros.value = it }
        }
        telemetryJobs += viewModelScope.launch {
            client.lastError.collect { connectionError.value = it }
        }

        // Sensor/network state remains hot while Compose receives display snapshots at ~30 Hz.
        uiSamplerJob = viewModelScope.launch {
            while (true) {
                val latest = liveState.snapshot()
                if (_controllerState.value != latest) {
                    _controllerState.value = latest
                }
                val measuredSensorRate = sensorHandler.eventRateHz
                if (sensorRateHz.value != measuredSensorRate) {
                    sensorRateHz.value = measuredSensorRate
                }
                delay(33)
            }
        }

        networkJob = viewModelScope.launch(Dispatchers.IO) {
            client.start()
        }
    }

    private fun stopControllerInternal(resetInputs: Boolean) {
        controllerRunning = false
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
        maxPacketGapMicros.value = 0L
        sensorRateHz.value = 0
        connectionError.value = null

        if (resetInputs) {
            currentSteeringAngleDeg = 0f
            liveState.reset()
            _controllerState.value = liveState.snapshot()
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
        val oldHandbrake = if (type == AnalogInput.HANDBRAKE) liveState.handbrakeValue() else 0f

        if (type == AnalogInput.HANDBRAKE && haptics.enabled) {
            if (oldHandbrake <= 0.001f && clamped > 0.001f) {
                haptics.handbrakePress()
            } else if (oldHandbrake > 0.001f && clamped <= 0.001f) {
                haptics.handbrakeRelease()
            }
        }

        val changed = when (type) {
            AnalogInput.THROTTLE -> liveState.setThrottle(clamped)
            AnalogInput.BRAKE -> liveState.setBrake(clamped)
            AnalogInput.CLUTCH -> liveState.setClutch(clamped)
            AnalogInput.HANDBRAKE -> liveState.setHandbrake(clamped)
        }
        signalTransportIfChanged(changed)
    }

    fun updateButton(type: ButtonInput, pressed: Boolean) {
        val mask = type.mask
        val wasPressed = liveState.isButtonPressed(mask)

        if (pressed && !wasPressed && haptics.enabled) {
            when (type) {
                ButtonInput.SHIFT_UP -> haptics.shiftUp()
                ButtonInput.SHIFT_DOWN -> haptics.shiftDown()
                ButtonInput.HORN -> haptics.horn()
                else -> Unit
            }
        }

        signalTransportIfChanged(liveState.setButton(mask, pressed))
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
        signalTransportIfChanged(liveState.setSteering(normalized))
    }

    private fun signalTransportIfChanged(changed: Boolean) {
        if (changed) udpClient?.requestImmediateSend()
    }

    override fun onCleared() {
        controllerRequested = false
        stopControllerInternal(resetInputs = true)
        sensorHandler.close()
        super.onCleared()
    }
}

enum class AnalogInput { THROTTLE, BRAKE, CLUTCH, HANDBRAKE }

enum class ButtonInput(val mask: Int) {
    SHIFT_UP(ControllerButtonBits.SHIFT_UP),
    SHIFT_DOWN(ControllerButtonBits.SHIFT_DOWN),
    HORN(ControllerButtonBits.HORN),
    CAMERA(ControllerButtonBits.CAMERA),
    RESET(ControllerButtonBits.RESET),
}
