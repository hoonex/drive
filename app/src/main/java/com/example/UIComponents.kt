package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2

@Composable
fun AnalogSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var y = down.position.y
                    onValueChange(calculateSliderValue(y, size.height.toFloat()))
                    
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            val pos = event.changes.first().position.y
                            onValueChange(calculateSliderValue(pos, size.height.toFloat()))
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // Reset on release
                    onValueChange(0f)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value)
                .align(Alignment.BottomCenter)
                .background(color)
        )
        Text(
            text = label,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

private fun calculateSliderValue(y: Float, height: Float): Float {
    return (1f - (y / height)).coerceIn(0f, 1f)
}

@Composable
fun ControllerButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 80.dp, minHeight = 80.dp)
            .clip(CircleShape)
            .background(if (isPressed) Color.LightGray else Color.DarkGray.copy(alpha = 0.7f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    onPress()
                    
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    
                    isPressed = false
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isPressed) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TouchWheel(
    currentAngle: Float, // displayed angle
    onAngleDelta: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var lastTouchAngle = Math.toDegrees(atan2((down.position.y - cy).toDouble(), (down.position.x - cx).toDouble())).toFloat()
                    
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            val pos = event.changes.first().position
                            val currentTouchAngle = Math.toDegrees(atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())).toFloat()
                            
                            var delta = currentTouchAngle - lastTouchAngle
                            if (delta > 180) delta -= 360
                            if (delta < -180) delta += 360
                            
                            onAngleDelta(delta)
                            lastTouchAngle = currentTouchAngle
                        }
                    } while (event.changes.any { it.pressed })
                    
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer { rotationZ = currentAngle }) {
            drawCircle(
                color = Color.DarkGray,
                radius = size.width / 2,
                style = Stroke(width = 24.dp.toPx())
            )
            // Left grip
            drawLine(
                color = Color.Red,
                start = Offset(24.dp.toPx(), center.y),
                end = center,
                strokeWidth = 8.dp.toPx()
            )
            // Right grip
            drawLine(
                color = Color.Red,
                start = Offset(size.width - 24.dp.toPx(), center.y),
                end = center,
                strokeWidth = 8.dp.toPx()
            )
            // Top marker
            drawLine(
                color = Color.Yellow,
                start = Offset(center.x, 0f),
                end = Offset(center.x, 32.dp.toPx()),
                strokeWidth = 8.dp.toPx()
            )
        }
        
        Text("${currentAngle.toInt()}°", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
