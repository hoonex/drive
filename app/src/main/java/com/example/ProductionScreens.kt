package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DriveAccent
import com.example.ui.theme.DriveAccentMuted
import com.example.ui.theme.DriveBackground
import com.example.ui.theme.DriveBorder
import com.example.ui.theme.DriveDanger
import com.example.ui.theme.DriveSuccess
import com.example.ui.theme.DriveSurface
import com.example.ui.theme.DriveSurfacePressed
import com.example.ui.theme.DriveSurfaceRaised
import com.example.ui.theme.DriveText
import com.example.ui.theme.DriveTextFaint
import com.example.ui.theme.DriveTextMuted
import com.example.ui.theme.DriveWarning
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionSettingsScreen(
    viewModel: ControllerViewModel,
    onBack: () -> Unit,
) {
    var steeringMode by remember { mutableStateOf(viewModel.settings.steeringMode) }
    var steeringRange by remember { mutableIntStateOf(viewModel.settings.steeringRange) }
    var tiltSensitivity by remember { mutableFloatStateOf(viewModel.settings.tiltSensitivity) }
    var returnMode by remember { mutableStateOf(viewModel.settings.touchReturnMode) }
    var pedalMode by remember { mutableStateOf(viewModel.uiSettings.pedalControlMode) }
    var wheelSide by remember { mutableStateOf(viewModel.uiSettings.touchWheelSide) }
    var lowLatency by remember { mutableStateOf(viewModel.settings.lowLatencyMode) }
    var haptics by remember { mutableStateOf(viewModel.settings.hapticsEnabled) }

    Scaffold(
        containerColor = DriveBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Controller settings", color = DriveText)
                        Text(
                            "Landscape profile · saved automatically",
                            color = DriveTextFaint,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = DriveText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DriveBackground),
            )
        },
    ) { scaffoldPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val narrow = maxWidth < 820.dp
            val sidePadding = if (maxHeight < 420.dp) 12.dp else 18.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = sidePadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (narrow) {
                    SteeringSettingsCard(
                        steeringMode = steeringMode,
                        onSteeringModeChanged = {
                            steeringMode = it
                            viewModel.settings.steeringMode = it
                            if (haptics) viewModel.haptics.modeChange()
                        },
                        steeringRange = steeringRange,
                        onSteeringRangeChanged = {
                            steeringRange = it
                            viewModel.settings.steeringRange = it
                        },
                        tiltSensitivity = tiltSensitivity,
                        onTiltSensitivityChanged = {
                            tiltSensitivity = it
                            viewModel.settings.tiltSensitivity = it
                        },
                        returnMode = returnMode,
                        onReturnModeChanged = {
                            returnMode = it
                            viewModel.settings.touchReturnMode = it
                        },
                        wheelSide = wheelSide,
                        onWheelSideChanged = {
                            wheelSide = it
                            viewModel.uiSettings.touchWheelSide = it
                        },
                    )

                    DrivingControlsCard(
                        pedalMode = pedalMode,
                        onPedalModeChanged = {
                            pedalMode = it
                            viewModel.uiSettings.pedalControlMode = it
                            if (haptics) viewModel.haptics.modeChange()
                        },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        SteeringSettingsCard(
                            steeringMode = steeringMode,
                            onSteeringModeChanged = {
                                steeringMode = it
                                viewModel.settings.steeringMode = it
                                if (haptics) viewModel.haptics.modeChange()
                            },
                            steeringRange = steeringRange,
                            onSteeringRangeChanged = {
                                steeringRange = it
                                viewModel.settings.steeringRange = it
                            },
                            tiltSensitivity = tiltSensitivity,
                            onTiltSensitivityChanged = {
                                tiltSensitivity = it
                                viewModel.settings.tiltSensitivity = it
                            },
                            returnMode = returnMode,
                            onReturnModeChanged = {
                                returnMode = it
                                viewModel.settings.touchReturnMode = it
                            },
                            wheelSide = wheelSide,
                            onWheelSideChanged = {
                                wheelSide = it
                                viewModel.uiSettings.touchWheelSide = it
                            },
                            modifier = Modifier.weight(1f),
                        )

                        DrivingControlsCard(
                            pedalMode = pedalMode,
                            onPedalModeChanged = {
                                pedalMode = it
                                viewModel.uiSettings.pedalControlMode = it
                                if (haptics) viewModel.haptics.modeChange()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                ProductionSettingsCard(
                    title = "Performance & display",
                    subtitle = "These options do not change the 36-byte PC protocol.",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingsToggleV2(
                        icon = { Icon(Icons.Rounded.Bolt, null, tint = DriveAccent) },
                        title = "Adaptive low latency",
                        description = "100 Hz heartbeat with early packets while controls are moving.",
                        checked = lowLatency,
                        onCheckedChange = {
                            lowLatency = it
                            viewModel.settings.lowLatencyMode = it
                        },
                    )
                    HorizontalDivider(color = DriveBorder.copy(alpha = 0.65f))
                    SettingsToggleV2(
                        icon = { Icon(Icons.Rounded.Vibration, null, tint = DriveAccent) },
                        title = "Haptic feedback",
                        description = "Tactile feedback for shifts, handbrake and recenter.",
                        checked = haptics,
                        onCheckedChange = {
                            haptics = it
                            viewModel.setHapticsEnabled(it)
                        },
                    )
                    HorizontalDivider(color = DriveBorder.copy(alpha = 0.65f))
                    InfoRowV2(
                        icon = { Icon(Icons.Rounded.Fullscreen, null, tint = DriveAccent) },
                        title = "Immersive fullscreen",
                        description = "Time, battery and navigation bars stay hidden. Swipe from an edge to reveal them temporarily.",
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SteeringSettingsCard(
    steeringMode: SteeringMode,
    onSteeringModeChanged: (SteeringMode) -> Unit,
    steeringRange: Int,
    onSteeringRangeChanged: (Int) -> Unit,
    tiltSensitivity: Float,
    onTiltSensitivityChanged: (Float) -> Unit,
    returnMode: ReturnMode,
    onReturnModeChanged: (ReturnMode) -> Unit,
    wheelSide: TouchWheelSide,
    onWheelSideChanged: (TouchWheelSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductionSettingsCard(
        title = "Steering",
        subtitle = "Choose the physical steering method and usable range.",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SteeringMode.entries.forEach { item ->
                FilterChip(
                    selected = steeringMode == item,
                    onClick = { onSteeringModeChanged(item) },
                    label = {
                        Text(
                            steeringModeLabel(item),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SettingLabelV2("Steering range", "$steeringRange°")
        Slider(
            value = steeringRange.toFloat(),
            onValueChange = { raw ->
                val snapped = ((raw / 90f).roundToInt() * 90).coerceIn(180, 1080)
                onSteeringRangeChanged(snapped)
            },
            valueRange = 180f..1080f,
            steps = 9,
        )

        if (steeringMode == SteeringMode.TILT) {
            SettingLabelV2("Tilt sensitivity", "${"%.1f".format(tiltSensitivity)}x")
            Slider(
                value = tiltSensitivity,
                onValueChange = onTiltSensitivityChanged,
                valueRange = 0.5f..3f,
            )
        }

        if (steeringMode == SteeringMode.TOUCH) {
            Spacer(Modifier.height(4.dp))
            Text("Wheel position", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TouchWheelSide.entries.forEach { side ->
                    FilterChip(
                        selected = wheelSide == side,
                        onClick = { onWheelSideChanged(side) },
                        label = { Text(if (side == TouchWheelSide.LEFT) "Left hand" else "Right hand") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Wheel return", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReturnMode.entries.forEach { item ->
                    FilterChip(
                        selected = returnMode == item,
                        onClick = { onReturnModeChanged(item) },
                        label = { Text(returnModeLabel(item), maxLines = 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrivingControlsCard(
    pedalMode: PedalControlMode,
    onPedalModeChanged: (PedalControlMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductionSettingsCard(
        title = "Driving controls",
        subtitle = "Pick a familiar arcade layout or keep precise analog pedals.",
        modifier = modifier,
    ) {
        Text("Pedals", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PedalControlMode.entries.forEach { item ->
                FilterChip(
                    selected = pedalMode == item,
                    onClick = { onPedalModeChanged(item) },
                    label = { Text(if (item == PedalControlMode.ARCADE) "Arcade tap" else "Analog slide") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Surface(
            color = DriveAccentMuted.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.TouchApp, null, tint = DriveAccent, modifier = Modifier.size(19.dp))
                Text(
                    text = if (pedalMode == PedalControlMode.ARCADE) {
                        "GAS sends full throttle while held. BRAKE / REV sends full brake, which racing games normally use for reverse once stopped."
                    } else {
                        "Vertical sliders preserve fine throttle, brake, clutch and handbrake control from 0–100%."
                    },
                    color = DriveTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun ProductionControllerScreen(
    viewModel: ControllerViewModel,
    onDisconnect: () -> Unit,
) {
    val state by viewModel.controllerState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val latency by viewModel.latency.collectAsStateWithLifecycle()
    val packetRate by viewModel.packetRate.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val localView = LocalView.current

    val steeringMode = viewModel.settings.steeringMode
    val steeringRange = viewModel.settings.steeringRange
    val pedalMode = viewModel.uiSettings.pedalControlMode
    val touchWheelSide = viewModel.uiSettings.touchWheelSide

    DisposableEffect(Unit) {
        localView.keepScreenOn = true
        viewModel.startController()
        onDispose {
            localView.keepScreenOn = false
            viewModel.stopController()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val compact = maxHeight < 430.dp || maxWidth < 850.dp
        val tiny = maxHeight < 350.dp || maxWidth < 680.dp
        val edge = if (compact) 8.dp else 14.dp
        val hasError = !isConnected && !connectionError.isNullOrBlank()
        val topReserved = when {
            hasError && tiny -> 76.dp
            hasError -> 84.dp
            tiny -> 44.dp
            else -> 52.dp
        }
        val bottomReserved = if (tiny) 54.dp else if (compact) 62.dp else 72.dp

        ProductionHud(
            isConnected = isConnected,
            latency = latency,
            packetRate = packetRate,
            mode = steeringMode,
            steering = state.steering,
            compact = compact,
            tiny = tiny,
            onDisconnect = onDisconnect,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = if (tiny) 4.dp else 6.dp),
        )

        if (hasError) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = edge + 6.dp)
                    .padding(top = if (tiny) 45.dp else 52.dp),
                color = DriveDanger.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DriveDanger.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = connectionError.orEmpty(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = DriveDanger,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = edge,
                    end = edge,
                    top = topReserved,
                    bottom = bottomReserved,
                ),
        ) {
            if (steeringMode == SteeringMode.TOUCH) {
                TouchDrivingLayout(
                    viewModel = viewModel,
                    state = state,
                    steeringRange = steeringRange,
                    pedalMode = pedalMode,
                    wheelSide = touchWheelSide,
                    compact = compact,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MotionDrivingLayout(
                    viewModel = viewModel,
                    state = state,
                    steeringMode = steeringMode,
                    steeringRange = steeringRange,
                    pedalMode = pedalMode,
                    compact = compact,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        ActionBarV2(
            viewModel = viewModel,
            compact = compact,
            tiny = tiny,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = if (tiny) 4.dp else 7.dp),
        )
    }
}

@Composable
private fun MotionDrivingLayout(
    viewModel: ControllerViewModel,
    state: ControllerState,
    steeringMode: SteeringMode,
    steeringRange: Int,
    pedalMode: PedalControlMode,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pedalMode == PedalControlMode.ANALOG) {
            AnalogPedalPair(
                firstValue = state.clutch,
                firstInput = AnalogInput.CLUTCH,
                firstColor = Color(0xFF4FA7FF),
                firstLabel = "CLUTCH",
                secondValue = state.throttle,
                secondInput = AnalogInput.THROTTLE,
                secondColor = Color(0xFF58D68D),
                secondLabel = "GAS",
                viewModel = viewModel,
                compact = compact,
            )
        } else {
            DigitalPedalButton(
                label = "GAS",
                hint = "FORWARD",
                accent = Color(0xFF58D68D),
                onPressedChanged = { pressed ->
                    viewModel.updateAnalog(AnalogInput.THROTTLE, if (pressed) 1f else 0f)
                },
                modifier = Modifier
                    .width(if (compact) 108.dp else 132.dp)
                    .fillMaxHeight(),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            SteeringReadoutV2(
                steering = state.steering,
                range = steeringRange,
                mode = steeringMode,
                sensorAvailable = if (steeringMode == SteeringMode.MOTION) {
                    viewModel.sensorHandler.hasRotationSensor
                } else {
                    viewModel.sensorHandler.hasAccelSensor
                },
                compact = compact,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (pedalMode == PedalControlMode.ANALOG) {
            AnalogPedalPair(
                firstValue = state.brake,
                firstInput = AnalogInput.BRAKE,
                firstColor = Color(0xFFFF6675),
                firstLabel = "BRAKE",
                secondValue = state.handbrake,
                secondInput = AnalogInput.HANDBRAKE,
                secondColor = Color(0xFFFFC95B),
                secondLabel = "E-BRAKE",
                viewModel = viewModel,
                compact = compact,
            )
        } else {
            Column(
                modifier = Modifier
                    .width(if (compact) 118.dp else 142.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DigitalPedalButton(
                    label = "BRAKE",
                    hint = "REVERSE",
                    accent = Color(0xFFFF6675),
                    onPressedChanged = { pressed ->
                        viewModel.updateAnalog(AnalogInput.BRAKE, if (pressed) 1f else 0f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                DigitalPedalButton(
                    label = "E-BRAKE",
                    hint = null,
                    accent = Color(0xFFFFC95B),
                    onPressedChanged = { pressed ->
                        viewModel.updateAnalog(AnalogInput.HANDBRAKE, if (pressed) 1f else 0f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 48.dp else 56.dp),
                )
            }
        }
    }
}

@Composable
private fun TouchDrivingLayout(
    viewModel: ControllerViewModel,
    state: ControllerState,
    steeringRange: Int,
    pedalMode: PedalControlMode,
    wheelSide: TouchWheelSide,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (wheelSide == TouchWheelSide.LEFT) {
            TouchWheelPane(viewModel, state, steeringRange, compact, Modifier.weight(1.08f))
            TouchPedalPane(viewModel, state, pedalMode, compact, Modifier.weight(0.92f))
        } else {
            TouchPedalPane(viewModel, state, pedalMode, compact, Modifier.weight(0.92f))
            TouchWheelPane(viewModel, state, steeringRange, compact, Modifier.weight(1.08f))
        }
    }
}

@Composable
private fun TouchWheelPane(
    viewModel: ControllerViewModel,
    state: ControllerState,
    steeringRange: Int,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        TouchWheel(
            currentAngle = state.steering * (steeringRange / 2f),
            onAngleDelta = viewModel::handleTouchWheelDelta,
            onRelease = viewModel::handleTouchWheelRelease,
            modifier = Modifier.fillMaxHeight(if (compact) 0.96f else 0.98f),
        )
    }
}

@Composable
private fun TouchPedalPane(
    viewModel: ControllerViewModel,
    state: ControllerState,
    pedalMode: PedalControlMode,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    if (pedalMode == PedalControlMode.ANALOG) {
        Row(
            modifier = modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sliderWidth = if (compact) 50.dp else 60.dp
            AnalogSlider(
                value = state.clutch,
                onValueChange = { viewModel.updateAnalog(AnalogInput.CLUTCH, it) },
                color = Color(0xFF4FA7FF),
                label = "CLUTCH",
                modifier = Modifier.width(sliderWidth).fillMaxHeight(),
            )
            AnalogSlider(
                value = state.throttle,
                onValueChange = { viewModel.updateAnalog(AnalogInput.THROTTLE, it) },
                color = Color(0xFF58D68D),
                label = "GAS",
                modifier = Modifier.width(sliderWidth).fillMaxHeight(),
            )
            AnalogSlider(
                value = state.brake,
                onValueChange = { viewModel.updateAnalog(AnalogInput.BRAKE, it) },
                color = Color(0xFFFF6675),
                label = "BRAKE",
                modifier = Modifier.width(sliderWidth).fillMaxHeight(),
            )
            AnalogSlider(
                value = state.handbrake,
                onValueChange = { viewModel.updateAnalog(AnalogInput.HANDBRAKE, it) },
                color = Color(0xFFFFC95B),
                label = "E-BRAKE",
                modifier = Modifier.width(sliderWidth).fillMaxHeight(),
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
            ) {
                DigitalPedalButton(
                    label = "GAS",
                    hint = "FORWARD",
                    accent = Color(0xFF58D68D),
                    onPressedChanged = { pressed ->
                        viewModel.updateAnalog(AnalogInput.THROTTLE, if (pressed) 1f else 0f)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                DigitalPedalButton(
                    label = "BRAKE",
                    hint = "REVERSE",
                    accent = Color(0xFFFF6675),
                    onPressedChanged = { pressed ->
                        viewModel.updateAnalog(AnalogInput.BRAKE, if (pressed) 1f else 0f)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            DigitalPedalButton(
                label = "E-BRAKE",
                hint = null,
                accent = Color(0xFFFFC95B),
                onPressedChanged = { pressed ->
                    viewModel.updateAnalog(AnalogInput.HANDBRAKE, if (pressed) 1f else 0f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 46.dp else 54.dp),
            )
        }
    }
}

@Composable
private fun AnalogPedalPair(
    firstValue: Float,
    firstInput: AnalogInput,
    firstColor: Color,
    firstLabel: String,
    secondValue: Float,
    secondInput: AnalogInput,
    secondColor: Color,
    secondLabel: String,
    viewModel: ControllerViewModel,
    compact: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
    ) {
        val sliderWidth = if (compact) 54.dp else 66.dp
        AnalogSlider(
            value = firstValue,
            onValueChange = { viewModel.updateAnalog(firstInput, it) },
            color = firstColor,
            label = firstLabel,
            modifier = Modifier.width(sliderWidth).fillMaxHeight(),
        )
        AnalogSlider(
            value = secondValue,
            onValueChange = { viewModel.updateAnalog(secondInput, it) },
            color = secondColor,
            label = secondLabel,
            modifier = Modifier.width(sliderWidth).fillMaxHeight(),
        )
    }
}

@Composable
private fun DigitalPedalButton(
    label: String,
    hint: String?,
    accent: Color,
    onPressedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (pressed) accent.copy(alpha = 0.28f) else DriveSurfaceRaised)
            .border(
                width = if (pressed) 2.dp else 1.dp,
                color = if (pressed) accent else DriveBorder,
                shape = shape,
            )
            .pointerInput(label) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onPressedChanged(true)
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                        }
                    } finally {
                        pressed = false
                        onPressedChanged(false)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (pressed) accent else DriveText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    color = if (pressed) accent.copy(alpha = 0.86f) else DriveTextFaint,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SteeringReadoutV2(
    steering: Float,
    range: Int,
    mode: SteeringMode,
    sensorAvailable: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val angle = steering * (range / 2f)
    val direction = when {
        abs(steering) < 0.015f -> "CENTER"
        steering < 0f -> "LEFT"
        else -> "RIGHT"
    }

    Surface(
        modifier = modifier,
        color = DriveSurface,
        shape = RoundedCornerShape(if (compact) 20.dp else 26.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                steeringModeLabel(mode).uppercase(),
                color = DriveTextFaint,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
            Text(
                text = "${angle.roundToInt()}°",
                color = DriveText,
                fontSize = if (compact) 40.sp else 58.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text(
                direction,
                color = if (abs(steering) < 0.015f) DriveTextFaint else DriveAccent,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.0.sp,
            )
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            SteeringMeterV2(steering)
            if (!sensorAvailable) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "Steering sensor unavailable",
                    color = DriveDanger,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SteeringMeterV2(steering: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val y = size.height / 2f
        val stroke = 6.dp.toPx()
        drawLine(
            color = DriveSurfaceRaised,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val centerX = size.width / 2f
        val targetX = centerX + centerX * steering.coerceIn(-1f, 1f)
        drawLine(
            color = DriveAccent,
            start = Offset(centerX, y),
            end = Offset(targetX, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DriveTextFaint,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ProductionHud(
    isConnected: Boolean,
    latency: Long,
    packetRate: Int,
    mode: SteeringMode,
    steering: Float,
    compact: Boolean,
    tiny: Boolean,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = DriveSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(if (tiny) 14.dp else 17.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChipV2(
                label = if (isConnected) "LIVE" else "LINK",
                value = null,
                color = if (isConnected) DriveSuccess else DriveWarning,
                tiny = tiny,
            )
            StatusChipV2(
                label = "RTT",
                value = if (isConnected) "${latency}ms" else "—",
                color = latencyColorV2(latency, isConnected),
                tiny = tiny,
            )
            if (!tiny) {
                StatusChipV2(
                    label = "TX",
                    value = if (packetRate > 0) "$packetRate" else "—",
                    color = DriveAccent,
                    tiny = false,
                )
            }
            if (!compact) {
                StatusChipV2(
                    label = "MODE",
                    value = steeringModeLabel(mode).uppercase(),
                    color = DriveTextMuted,
                    tiny = false,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${if (steering >= 0f) "+" else ""}${(steering * 100f).roundToInt()}%",
                color = DriveText,
                style = if (tiny) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier.size(if (tiny) 34.dp else 38.dp),
            ) {
                Icon(
                    Icons.Rounded.PowerSettingsNew,
                    "Exit controller",
                    tint = DriveTextMuted,
                    modifier = Modifier.size(if (tiny) 18.dp else 21.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionBarV2(
    viewModel: ControllerViewModel,
    compact: Boolean,
    tiny: Boolean,
    modifier: Modifier = Modifier,
) {
    val shiftWidth = if (tiny) 70.dp else if (compact) 82.dp else 104.dp
    val centerWidth = if (tiny) 62.dp else if (compact) 72.dp else 84.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControllerButton(
            label = if (tiny) "−" else "SHIFT −",
            onPress = { viewModel.updateButton(ButtonInput.SHIFT_DOWN, true) },
            onRelease = { viewModel.updateButton(ButtonInput.SHIFT_DOWN, false) },
            modifier = Modifier.width(shiftWidth),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(if (tiny) 4.dp else 7.dp)) {
            ControllerButton(
                label = "HORN",
                onPress = { viewModel.updateButton(ButtonInput.HORN, true) },
                onRelease = { viewModel.updateButton(ButtonInput.HORN, false) },
                modifier = Modifier.width(centerWidth),
            )
            ControllerButton(
                label = if (tiny) "CTR" else "CENTER",
                onPress = {
                    viewModel.updateButton(ButtonInput.RESET, true)
                    viewModel.calibrate()
                },
                onRelease = { viewModel.updateButton(ButtonInput.RESET, false) },
                modifier = Modifier.width(centerWidth),
            )
            ControllerButton(
                label = "CAM",
                onPress = { viewModel.updateButton(ButtonInput.CAMERA, true) },
                onRelease = { viewModel.updateButton(ButtonInput.CAMERA, false) },
                modifier = Modifier.width(centerWidth),
            )
        }

        ControllerButton(
            label = if (tiny) "+" else "SHIFT +",
            onPress = { viewModel.updateButton(ButtonInput.SHIFT_UP, true) },
            onRelease = { viewModel.updateButton(ButtonInput.SHIFT_UP, false) },
            modifier = Modifier.width(shiftWidth),
        )
    }
}

@Composable
private fun ProductionSettingsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = DriveSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = DriveTextMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleV2(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = DriveSurfaceRaised,
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleMedium)
            Text(description, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRowV2(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = DriveSurfaceRaised,
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleMedium)
            Text(description, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingLabelV2(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        Text(value, color = DriveAccent, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatusChipV2(
    label: String,
    value: String?,
    color: Color,
    tiny: Boolean,
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (tiny) 7.dp else 9.dp,
                vertical = if (tiny) 3.dp else 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            if (value != null) {
                Text(value, color = DriveText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun steeringModeLabel(mode: SteeringMode): String = when (mode) {
    SteeringMode.MOTION -> "Motion"
    SteeringMode.TILT -> "Tilt"
    SteeringMode.TOUCH -> "Touch"
}

private fun returnModeLabel(mode: ReturnMode): String = when (mode) {
    ReturnMode.SMOOTH -> "Smooth"
    ReturnMode.INSTANT -> "Instant"
    ReturnMode.HOLD -> "Hold"
}

private fun latencyColorV2(latency: Long, connected: Boolean): Color = when {
    !connected -> DriveTextFaint
    latency <= 12L -> DriveSuccess
    latency <= 30L -> DriveAccent
    latency <= 60L -> DriveWarning
    else -> DriveDanger
}
