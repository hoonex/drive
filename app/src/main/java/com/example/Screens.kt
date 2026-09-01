package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.example.ui.theme.DriveSurfaceRaised
import com.example.ui.theme.DriveText
import com.example.ui.theme.DriveTextFaint
import com.example.ui.theme.DriveTextMuted
import com.example.ui.theme.DriveWarning
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ConnectionScreen(
    viewModel: ControllerViewModel,
    onNavigateToController: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var ip by rememberSaveable { mutableStateOf(viewModel.settings.ip) }
    var port by rememberSaveable { mutableStateOf(viewModel.settings.port.toString()) }
    val parsedPort = port.toIntOrNull()
    val canConnect = ip.trim().isNotEmpty() && parsedPort != null && parsedPort in 1..65535

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(0.95f),
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = DriveAccentMuted,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = DriveAccent,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "PC WHEEL",
                            color = DriveAccent,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Drive with your phone.",
                    color = DriveText,
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Motion steering, analog pedals and low-latency UDP in one controller.",
                    color = DriveTextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.widthIn(max = 470.dp),
                )
                Spacer(Modifier.height(22.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureChip(
                        icon = { Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(15.dp)) },
                        text = if (viewModel.settings.lowLatencyMode) "Adaptive low latency" else "100 Hz balanced",
                    )
                    FeatureChip(
                        icon = { Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(15.dp)) },
                        text = "${viewModel.settings.steeringRange}° range",
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1.05f)
                    .widthIn(max = 520.dp),
                color = DriveSurface,
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DriveBorder),
                tonalElevation = 0.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Connect to PC",
                                color = DriveText,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(
                                text = "PC Receiver must be running on the same network.",
                                color = DriveTextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = DriveTextMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("PC IPv4 address") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Computer, contentDescription = null)
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all(Char::isDigit)) port = value
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("UDP port") },
                        supportingText = { Text("PC Receiver default: 26760") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(Icons.Rounded.Wifi, contentDescription = null)
                        },
                        isError = port.isNotEmpty() && (parsedPort == null || parsedPort !in 1..65535),
                    )

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            viewModel.settings.ip = ip
                            viewModel.settings.port = parsedPort ?: 26760
                            onNavigateToController()
                        },
                        enabled = canConnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DriveAccent,
                            contentColor = DriveBackground,
                        ),
                    ) {
                        Text("Start controller", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ControllerViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(viewModel.settings.steeringMode) }
    var range by remember { mutableIntStateOf(viewModel.settings.steeringRange) }
    var sensitivity by remember { mutableFloatStateOf(viewModel.settings.tiltSensitivity) }
    var returnMode by remember { mutableStateOf(viewModel.settings.touchReturnMode) }
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
                            "Changes are saved immediately",
                            color = DriveTextFaint,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = DriveText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DriveBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsCard(
                    title = "Steering",
                    subtitle = "Choose how the phone controls the steering axis.",
                    modifier = Modifier.weight(1f),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SteeringMode.values().forEach { item ->
                            FilterChip(
                                selected = mode == item,
                                onClick = {
                                    mode = item
                                    viewModel.settings.steeringMode = item
                                    if (haptics) viewModel.haptics.modeChange()
                                },
                                label = { Text(item.displayName()) },
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    SettingLabel("Steering range", "$range°")
                    Slider(
                        value = range.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 90f).roundToInt() * 90).coerceIn(180, 1080)
                            range = snapped
                            viewModel.settings.steeringRange = snapped
                        },
                        valueRange = 180f..1080f,
                        steps = 9,
                    )

                    if (mode == SteeringMode.TILT) {
                        Spacer(Modifier.height(10.dp))
                        SettingLabel("Tilt sensitivity", "${"%.1f".format(sensitivity)}x")
                        Slider(
                            value = sensitivity,
                            onValueChange = {
                                sensitivity = it
                                viewModel.settings.tiltSensitivity = it
                            },
                            valueRange = 0.5f..3f,
                        )
                    }

                    if (mode == SteeringMode.TOUCH) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Touch wheel return",
                            color = DriveTextMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReturnMode.values().forEach { item ->
                                FilterChip(
                                    selected = returnMode == item,
                                    onClick = {
                                        returnMode = item
                                        viewModel.settings.touchReturnMode = item
                                    },
                                    label = { Text(item.displayName()) },
                                )
                            }
                        }
                    }
                }

                SettingsCard(
                    title = "Performance & feedback",
                    subtitle = "Latency-sensitive options only affect the controller path.",
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsToggleRow(
                        icon = { Icon(Icons.Rounded.Bolt, null, tint = DriveAccent) },
                        title = "Adaptive low latency",
                        description = "100 Hz heartbeat with input-triggered fast packets while steering.",
                        checked = lowLatency,
                        onCheckedChange = {
                            lowLatency = it
                            viewModel.settings.lowLatencyMode = it
                        },
                    )
                    HorizontalDivider(color = DriveBorder.copy(alpha = 0.65f))
                    SettingsToggleRow(
                        icon = { Icon(Icons.Rounded.Vibration, null, tint = DriveAccent) },
                        title = "Haptic feedback",
                        description = "Shift, handbrake and center confirmation feedback.",
                        checked = haptics,
                        onCheckedChange = {
                            haptics = it
                            viewModel.setHapticsEnabled(it)
                        },
                    )

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = DriveAccentMuted.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Rounded.Speed, null, tint = DriveAccent, modifier = Modifier.size(19.dp))
                            Text(
                                text = "Low latency uses the fastest unbatched sensor stream on a dedicated sensor thread. UI rendering is sampled separately so it cannot block steering updates.",
                                color = DriveTextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun ControllerScreen(viewModel: ControllerViewModel, onDisconnect: () -> Unit) {
    val state by viewModel.controllerState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val latency by viewModel.latency.collectAsStateWithLifecycle()
    val packetRate by viewModel.packetRate.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val localView = LocalView.current
    val mode = viewModel.settings.steeringMode
    val range = viewModel.settings.steeringRange

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
        val compact = maxHeight < 420.dp
        val pedalHeight = if (compact) 205.dp else 285.dp
        val centerHeight = if (compact) 205.dp else 285.dp
        val bottomPadding = if (compact) 8.dp else 14.dp

        ControllerHud(
            isConnected = isConnected,
            latency = latency,
            packetRate = packetRate,
            mode = mode,
            steering = state.steering,
            onDisconnect = onDisconnect,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (compact) 44.dp else 54.dp,
                    bottom = if (compact) 66.dp else 82.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.height(pedalHeight),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            ) {
                AnalogSlider(
                    value = state.clutch,
                    onValueChange = { viewModel.updateAnalog(AnalogInput.CLUTCH, it) },
                    color = Color(0xFF4FA7FF),
                    label = "CLUTCH",
                )
                AnalogSlider(
                    value = state.throttle,
                    onValueChange = { viewModel.updateAnalog(AnalogInput.THROTTLE, it) },
                    color = Color(0xFF58D68D),
                    label = "GAS",
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(centerHeight),
                contentAlignment = Alignment.Center,
            ) {
                when (mode) {
                    SteeringMode.TOUCH -> TouchWheel(
                        currentAngle = state.steering * (range / 2f),
                        onAngleDelta = viewModel::handleTouchWheelDelta,
                        onRelease = viewModel::handleTouchWheelRelease,
                        modifier = Modifier.fillMaxHeight(),
                    )

                    SteeringMode.MOTION,
                    SteeringMode.TILT,
                    -> SteeringReadout(
                        steering = state.steering,
                        range = range,
                        mode = mode,
                        sensorAvailable = when (mode) {
                            SteeringMode.MOTION -> viewModel.sensorHandler.hasRotationSensor
                            SteeringMode.TILT -> viewModel.sensorHandler.hasAccelSensor
                            SteeringMode.TOUCH -> true
                        },
                        compact = compact,
                    )
                }
            }

            Row(
                modifier = Modifier.height(pedalHeight),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            ) {
                AnalogSlider(
                    value = state.brake,
                    onValueChange = { viewModel.updateAnalog(AnalogInput.BRAKE, it) },
                    color = Color(0xFFFF6675),
                    label = "BRAKE",
                )
                AnalogSlider(
                    value = state.handbrake,
                    onValueChange = { viewModel.updateAnalog(AnalogInput.HANDBRAKE, it) },
                    color = Color(0xFFFFC95B),
                    label = "E-BRAKE",
                )
            }
        }

        if (connectionError != null && !isConnected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (compact) 54.dp else 62.dp),
                color = DriveDanger.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DriveDanger.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = connectionError ?: "",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = DriveDanger,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = bottomPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControllerButton(
                label = "SHIFT −",
                onPress = { viewModel.updateButton(ButtonInput.SHIFT_DOWN, true) },
                onRelease = { viewModel.updateButton(ButtonInput.SHIFT_DOWN, false) },
                modifier = Modifier.width(if (compact) 92.dp else 108.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
                ControllerButton(
                    label = "HORN",
                    onPress = { viewModel.updateButton(ButtonInput.HORN, true) },
                    onRelease = { viewModel.updateButton(ButtonInput.HORN, false) },
                )
                ControllerButton(
                    label = "CENTER",
                    onPress = {
                        viewModel.updateButton(ButtonInput.RESET, true)
                        viewModel.calibrate()
                    },
                    onRelease = { viewModel.updateButton(ButtonInput.RESET, false) },
                )
                ControllerButton(
                    label = "CAM",
                    onPress = { viewModel.updateButton(ButtonInput.CAMERA, true) },
                    onRelease = { viewModel.updateButton(ButtonInput.CAMERA, false) },
                )
            }

            ControllerButton(
                label = "SHIFT +",
                onPress = { viewModel.updateButton(ButtonInput.SHIFT_UP, true) },
                onRelease = { viewModel.updateButton(ButtonInput.SHIFT_UP, false) },
                modifier = Modifier.width(if (compact) 92.dp else 108.dp),
            )
        }
    }
}

@Composable
private fun ControllerHud(
    isConnected: Boolean,
    latency: Long,
    packetRate: Int,
    mode: SteeringMode,
    steering: Float,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = DriveSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DriveBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(
                label = if (isConnected) "CONNECTED" else "CONNECTING",
                value = null,
                color = if (isConnected) DriveSuccess else DriveWarning,
            )
            StatusPill("RTT", if (isConnected) "${latency} ms" else "—", latencyColor(latency, isConnected))
            StatusPill("TX", if (packetRate > 0) "$packetRate Hz" else "—", DriveAccent)
            StatusPill("MODE", mode.displayName().uppercase(), DriveTextMuted)

            Spacer(Modifier.weight(1f))
            Text(
                text = "STEER ${if (steering >= 0f) "+" else ""}${(steering * 100f).roundToInt()}%",
                color = DriveText,
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(onClick = onDisconnect, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = "Exit controller",
                    tint = DriveTextMuted,
                )
            }
        }
    }
}

@Composable
private fun SteeringReadout(
    steering: Float,
    range: Int,
    mode: SteeringMode,
    sensorAvailable: Boolean,
    compact: Boolean,
) {
    val angle = steering * (range / 2f)
    val direction = when {
        abs(steering) < 0.015f -> "CENTER"
        steering < 0f -> "LEFT"
        else -> "RIGHT"
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.78f),
        color = DriveSurface,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DriveBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = if (compact) 16.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mode.displayName().uppercase(),
                color = DriveTextFaint,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            Text(
                text = "${angle.roundToInt()}°",
                color = DriveText,
                fontSize = if (compact) 46.sp else 64.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text(
                text = direction,
                color = if (abs(steering) < 0.015f) DriveTextFaint else DriveAccent,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            SteeringMeter(steering = steering)

            if (!sensorAvailable) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Required sensor is not available on this device.",
                    color = DriveDanger,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun SteeringMeter(steering: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(DriveSurfaceRaised, RoundedCornerShape(999.dp)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .fillMaxHeight()
                .background(DriveTextFaint),
        )
        val widthFraction = abs(steering).coerceIn(0f, 1f) / 2f
        if (widthFraction > 0f) {
            Box(
                modifier = Modifier
                    .align(if (steering < 0f) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
                    .background(DriveAccent, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun FeatureChip(
    icon: @Composable () -> Unit,
    text: String,
) {
    Surface(
        color = DriveSurfaceRaised,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DriveBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) { icon() }
            Text(text, color = DriveTextMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable Column.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = DriveSurface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DriveBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = DriveTextMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun SettingLabel(label: String, value: String) {
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
private fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = DriveSurfaceRaised,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleMedium)
            Text(description, color = DriveTextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String?,
    color: Color,
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = color, style = MaterialTheme.typography.labelMedium)
            if (value != null) {
                Text(value, color = DriveText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun SteeringMode.displayName(): String = when (this) {
    SteeringMode.MOTION -> "Motion wheel"
    SteeringMode.TILT -> "Tilt"
    SteeringMode.TOUCH -> "Touch wheel"
}

private fun ReturnMode.displayName(): String = when (this) {
    ReturnMode.SMOOTH -> "Smooth"
    ReturnMode.INSTANT -> "Instant"
    ReturnMode.HOLD -> "Hold"
}

private fun latencyColor(latency: Long, connected: Boolean): Color = when {
    !connected -> DriveTextFaint
    latency <= 12L -> DriveSuccess
    latency <= 30L -> DriveAccent
    latency <= 60L -> DriveWarning
    else -> DriveDanger
}
