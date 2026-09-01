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
}
