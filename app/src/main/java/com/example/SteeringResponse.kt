package com.example

import kotlin.math.abs
import kotlin.math.pow

internal fun applySteeringResponse(
    raw: Float,
    deadzone: Float,
    response: Float,
    inverted: Boolean,
): Float {
    val clampedRaw = raw.coerceIn(-1f, 1f)
    val dz = deadzone.coerceIn(0f, 0.95f)
    val magnitude = abs(clampedRaw)
    val normalized = if (magnitude <= dz) {
        0f
    } else {
        ((magnitude - dz) / (1f - dz)).coerceIn(0f, 1f)
    }
    val curved = normalized.toDouble()
        .pow(response.coerceIn(0.2f, 4f).toDouble())
        .toFloat()
    val signed = if (clampedRaw < 0f) -curved else curved
    return (if (inverted) -signed else signed).coerceIn(-1f, 1f)
}