package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ControllerViewModel(application: Application) : AndroidViewModel(application) {
    val settings = SettingsRepository(application)
    val sensorHandler = SensorHandler(application)
    val haptics = HapticManager(application)

    private var udpClient: UdpClient? = null
    private var clientJob: Job? = null
    
    private val _controllerState = MutableStateFlow(ControllerState())
    val controllerState: StateFlow<ControllerState> = _controllerState.asStateFlow()

    // UI exposed states
    val isConnected = MutableStateFlow(false)
    val latency = MutableStateFlow(0L)
    val packetRate = MutableStateFlow(0)
    
    var currentSteeringAngleDeg = 0f
    
    // Smooth return job for Touch Wheel
    private var returnJob: Job? = null

    init {
        sensorHandler.onMotionAngleChanged = { angle ->
            if (settings.steeringMode == SteeringMode.MOTION) {
                // Invert angle to make physical clockwise rotation steer right (positive)
                updateSteeringFromAngle(-angle)
            }
        }
        sensorHandler.onTiltAngleChanged = { angle ->
            if (settings.steeringMode == SteeringMode.TILT) {
                updateSteeringFromAngle(angle * settings.tiltSensitivity)
            }
        }
    }

    private fun updateSteeringFromAngle(angle: Float) {
        currentSteeringAngleDeg = angle
        val range = settings.steeringRange.toFloat()
        // angle is in degrees, range is total range (e.g. 900). Max physical is range/2
        val normalized = (angle / (range / 2f)).coerceIn(-1f, 1f)
        _controllerState.update { it.copy(steering = normalized) }
    }

    fun startController() {
        sensorHandler.start()
        
        udpClient?.close()
        udpClient = UdpClient(settings.ip, settings.port)
        
        viewModelScope.launch {
            udpClient?.isConnected?.collect { isConnected.value = it }
        }
        viewModelScope.launch {
            udpClient?.latency?.collect { latency.value = it }
        }
        viewModelScope.launch {
            udpClient?.packetRate?.collect { packetRate.value = it }
        }

        clientJob?.cancel()
        clientJob = viewModelScope.launch {
            udpClient?.start(_controllerState)
        }
    }

    fun stopController() {
        sensorHandler.stop()
        clientJob?.cancel()
        udpClient?.close()
        isConnected.value = false
        latency.value = 0
        packetRate.value = 0
    }

    fun calibrate() {
        sensorHandler.calibrate()
        if (settings.steeringMode == SteeringMode.TOUCH) {
            updateSteeringFromAngle(0f)
        }
        haptics.recenter()
    }

    // Input handlers
    fun updateAnalog(type: AnalogInput, value: Float) {
        val oldState = _controllerState.value
        if (type == AnalogInput.HANDBRAKE) {
            if (oldState.handbrake == 0f && value > 0f) haptics.handbrakePress()
            else if (oldState.handbrake > 0f && value == 0f) haptics.handbrakeRelease()
        }
        _controllerState.update { state ->
            when(type) {
                AnalogInput.THROTTLE -> state.copy(throttle = value)
                AnalogInput.BRAKE -> state.copy(brake = value)
                AnalogInput.CLUTCH -> state.copy(clutch = value)
                AnalogInput.HANDBRAKE -> state.copy(handbrake = value)
            }
        }
    }

    fun updateButton(type: ButtonInput, pressed: Boolean) {
        val oldState = _controllerState.value
        if (pressed) {
            when (type) {
                ButtonInput.SHIFT_UP -> if (!oldState.shiftUp) haptics.shiftUp()
                ButtonInput.SHIFT_DOWN -> if (!oldState.shiftDown) haptics.shiftDown()
                ButtonInput.HORN -> if (!oldState.horn) haptics.horn()
                else -> {}
            }
        }
        _controllerState.update { state ->
            when(type) {
                ButtonInput.SHIFT_UP -> state.copy(shiftUp = pressed)
                ButtonInput.SHIFT_DOWN -> state.copy(shiftDown = pressed)
                ButtonInput.HORN -> state.copy(horn = pressed)
                ButtonInput.CAMERA -> state.copy(camera = pressed)
                ButtonInput.RESET -> state.copy(reset = pressed)
            }
        }
    }

    // Touch wheel specific
    fun handleTouchWheelDelta(delta: Float) {
        returnJob?.cancel()
        updateSteeringFromAngle(currentSteeringAngleDeg + delta)
    }

    fun handleTouchWheelRelease() {
        when (settings.touchReturnMode) {
            ReturnMode.INSTANT -> {
                updateSteeringFromAngle(0f)
            }
            ReturnMode.SMOOTH -> {
                returnJob?.cancel()
                returnJob = viewModelScope.launch {
                    var angle = currentSteeringAngleDeg
                    while (Math.abs(angle) > 1f) {
                        angle *= 0.8f // decay
                        updateSteeringFromAngle(angle)
                        delay(16)
                    }
                    updateSteeringFromAngle(0f)
                }
            }
            ReturnMode.HOLD -> { /* Do nothing */ }
        }
    }
}

enum class AnalogInput { THROTTLE, BRAKE, CLUTCH, HANDBRAKE }
enum class ButtonInput { SHIFT_UP, SHIFT_DOWN, HORN, CAMERA, RESET }
