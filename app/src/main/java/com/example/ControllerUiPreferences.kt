package com.example

import android.content.Context

enum class PedalControlMode {
    ARCADE,
    ANALOG,
}

enum class TouchWheelSide {
    LEFT,
    RIGHT,
}

class ControllerUiPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("pc_wheel_settings", Context.MODE_PRIVATE)

    var pedalControlMode: PedalControlMode
        get() = runCatching {
            PedalControlMode.valueOf(
                prefs.getString("pedalControlMode", PedalControlMode.ARCADE.name)
                    ?: PedalControlMode.ARCADE.name,
            )
        }.getOrDefault(PedalControlMode.ARCADE)
        set(value) = prefs.edit().putString("pedalControlMode", value.name).apply()

    var touchWheelSide: TouchWheelSide
        get() = runCatching {
            TouchWheelSide.valueOf(
                prefs.getString("touchWheelSide", TouchWheelSide.LEFT.name)
                    ?: TouchWheelSide.LEFT.name,
            )
        }.getOrDefault(TouchWheelSide.LEFT)
        set(value) = prefs.edit().putString("touchWheelSide", value.name).apply()

    var steeringDeadzone: Float
        get() = prefs.getFloat("steeringDeadzone", 0.015f).coerceIn(0f, 0.12f)
        set(value) = prefs.edit().putFloat("steeringDeadzone", value.coerceIn(0f, 0.12f)).apply()

    var steeringResponse: Float
        get() = prefs.getFloat("steeringResponse", 1.0f).coerceIn(0.55f, 2.0f)
        set(value) = prefs.edit().putFloat("steeringResponse", value.coerceIn(0.55f, 2.0f)).apply()

    var invertSteering: Boolean
        get() = prefs.getBoolean("invertSteering", false)
        set(value) = prefs.edit().putBoolean("invertSteering", value).apply()

    var diagnosticsEnabled: Boolean
        get() = prefs.getBoolean("diagnosticsEnabled", true)
        set(value) = prefs.edit().putBoolean("diagnosticsEnabled", value).apply()

    var automaticUpdates: Boolean
        get() = prefs.getBoolean("automaticUpdates", true)
        set(value) = prefs.edit().putBoolean("automaticUpdates", value).apply()

    var updateWifiOnly: Boolean
        get() = prefs.getBoolean("updateWifiOnly", true)
        set(value) = prefs.edit().putBoolean("updateWifiOnly", value).apply()
}