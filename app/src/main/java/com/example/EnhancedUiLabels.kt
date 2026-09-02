package com.example

internal fun steeringModeLabel(mode: SteeringMode): String = when (mode) {
    SteeringMode.MOTION -> "Motion"
    SteeringMode.TILT -> "Tilt"
    SteeringMode.TOUCH -> "Touch"
}
