package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DriveAccent
import com.example.ui.theme.DriveBorder
import com.example.ui.theme.DriveSurfacePressed
import com.example.ui.theme.DriveSurfaceRaised
import com.example.ui.theme.DriveText
import com.example.ui.theme.DriveTextMuted
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun AnalogSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val clamped = value.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(70.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(DriveSurfaceRaised)
            .border(1.dp, DriveBorder, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onValueChange(calculateSliderValue(down.position.y, size.height.toFloat()))

                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        pressed = change.pressed
                        if (pressed) {
                            onValueChange(calculateSliderValue(change.position.y, size.height.toFloat()))
                            change.consume()
                        }
                    }
                    onValueChange(0f)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(clamped)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.56f), color),
                    ),
                ),
        )

        Text(
            text = "${(clamped * 100f).roundToInt()}",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            color = DriveText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            color = if (clamped > 0.35f) DriveText else DriveTextMuted,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.7.sp,
        )
    }
}

private fun calculateSliderValue(y: Float, height: Float): Float {
    if (height <= 0f) return 0f
    return (1f - (y / height)).coerceIn(0f, 1f)
}

@Composable
fun ControllerButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 76.dp, minHeight = 58.dp)
            .clip(shape)
            .background(if (isPressed) DriveSurfacePressed else DriveSurfaceRaised)
            .border(
                width = 1.dp,
                color = if (isPressed) DriveAccent.copy(alpha = 0.75f) else DriveBorder,
                shape = shape,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPress()

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                        }
                    } finally {
                        isPressed = false
                        onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = DriveText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun TouchWheel(
    currentAngle: Float,
    onAngleDelta: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(DriveSurfaceRaised.copy(alpha = 0.74f))
            .border(1.dp, DriveBorder, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var lastTouchAngle = Math.toDegrees(
                        atan2(
                            (down.position.y - cy).toDouble(),
                            (down.position.x - cx).toDouble(),
                        ),
                    ).toFloat()

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break

                            val position = change.position
                            val currentTouchAngle = Math.toDegrees(
                                atan2(
                                    (position.y - cy).toDouble(),
                                    (position.x - cx).toDouble(),
                                ),
                            ).toFloat()

                            var delta = currentTouchAngle - lastTouchAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            onAngleDelta(delta)
                            lastTouchAngle = currentTouchAngle
                            change.consume()
                        }
                    } finally {
                        onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp)
                .graphicsLayer { rotationZ = currentAngle },
        ) {
            val outerRadius = size.minDimension / 2f
            val ringWidth = 15.dp.toPx()

            drawCircle(
                color = Color(0xFF34404C),
                radius = outerRadius - ringWidth / 2f,
                style = Stroke(width = ringWidth),
            )
            drawCircle(
                color = DriveAccent.copy(alpha = 0.16f),
                radius = outerRadius - ringWidth * 1.45f,
                style = Stroke(width = 2.dp.toPx()),
            )

            val hubRadius = 24.dp.toPx()
            drawLine(
                color = Color(0xFF566573),
                start = Offset(center.x - outerRadius * 0.64f, center.y),
                end = Offset(center.x - hubRadius, center.y),
                strokeWidth = 8.dp.toPx(),
            )
            drawLine(
                color = Color(0xFF566573),
                start = Offset(center.x + hubRadius, center.y),
                end = Offset(center.x + outerRadius * 0.64f, center.y),
                strokeWidth = 8.dp.toPx(),
            )
            drawLine(
                color = Color(0xFF566573),
                start = Offset(center.x, center.y + hubRadius),
                end = Offset(center.x, center.y + outerRadius * 0.58f),
                strokeWidth = 8.dp.toPx(),
            )
            drawCircle(color = Color(0xFF26323D), radius = hubRadius)

            drawLine(
                color = DriveAccent,
                start = Offset(center.x, 2.dp.toPx()),
                end = Offset(center.x, 24.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
            )
        }

        Text(
            text = "${currentAngle.roundToInt()}°",
            color = DriveText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
