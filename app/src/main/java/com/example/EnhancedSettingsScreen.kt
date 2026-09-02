package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DriveAccent
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
import kotlin.math.roundToInt

@Composable
fun EnhancedSettingsScreen(
    viewModel: ControllerViewModel,
    onBack: () -> Unit,
) {
    var steeringMode by remember { mutableStateOf(viewModel.settings.steeringMode) }
    var steeringRange by remember { mutableIntStateOf(viewModel.settings.steeringRange) }
    var tiltSensitivity by remember { mutableFloatStateOf(viewModel.settings.tiltSensitivity) }
    var returnMode by remember { mutableStateOf(viewModel.settings.touchReturnMode) }
    var pedalMode by remember { mutableStateOf(viewModel.uiSettings.pedalControlMode) }
    var wheelSide by remember { mutableStateOf(viewModel.uiSettings.touchWheelSide) }
    var deadzone by remember { mutableFloatStateOf(viewModel.uiSettings.steeringDeadzone) }
    var response by remember { mutableFloatStateOf(viewModel.uiSettings.steeringResponse) }
    var invertSteering by remember { mutableStateOf(viewModel.uiSettings.invertSteering) }
    var diagnostics by remember { mutableStateOf(viewModel.uiSettings.diagnosticsEnabled) }
    var lowLatency by remember { mutableStateOf(viewModel.settings.lowLatencyMode) }
    var haptics by remember { mutableStateOf(viewModel.settings.hapticsEnabled) }
    var autoUpdates by remember { mutableStateOf(viewModel.uiSettings.automaticUpdates) }
    var wifiOnly by remember { mutableStateOf(viewModel.uiSettings.updateWifiOnly) }
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val short = maxHeight < 390.dp
        val twoColumns = maxWidth >= 900.dp && maxHeight >= 400.dp
        val edge = if (short) 10.dp else 16.dp
        val gap = if (short) 8.dp else 12.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = edge, vertical = if (short) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(if (short) 38.dp else 44.dp)) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = DriveText)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Controller setup",
                        color = DriveText,
                        style = if (short) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    )
                    if (!short) {
                        Text(
                            "Input response, layout, performance and updates",
                            color = DriveTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    "v${viewModel.currentAppVersionName()}",
                    color = DriveTextFaint,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = edge, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                if (twoColumns) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1.06f),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            SteeringTuningCard(
                                viewModel = viewModel,
                                steeringMode = steeringMode,
                                onSteeringModeChange = {
                                    steeringMode = it
                                    viewModel.settings.steeringMode = it
                                },
                                steeringRange = steeringRange,
                                onSteeringRangeChange = {
                                    steeringRange = it
                                    viewModel.settings.steeringRange = it
                                },
                                tiltSensitivity = tiltSensitivity,
                                onTiltSensitivityChange = {
                                    tiltSensitivity = it
                                    viewModel.settings.tiltSensitivity = it
                                },
                                returnMode = returnMode,
                                onReturnModeChange = {
                                    returnMode = it
                                    viewModel.settings.touchReturnMode = it
                                },
                                wheelSide = wheelSide,
                                onWheelSideChange = {
                                    wheelSide = it
                                    viewModel.uiSettings.touchWheelSide = it
                                },
                                deadzone = deadzone,
                                onDeadzoneChange = {
                                    deadzone = it
                                    viewModel.uiSettings.steeringDeadzone = it
                                },
                                response = response,
                                onResponseChange = {
                                    response = it
                                    viewModel.uiSettings.steeringResponse = it
                                },
                                invertSteering = invertSteering,
                                onInvertChange = {
                                    invertSteering = it
                                    viewModel.uiSettings.invertSteering = it
                                },
                                compact = short,
                            )
                        }

                        Column(
                            modifier = Modifier.weight(0.94f),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            DriveControlsCard(
                                viewModel = viewModel,
                                pedalMode = pedalMode,
                                onPedalModeChange = {
                                    pedalMode = it
                                    viewModel.uiSettings.pedalControlMode = it
                                },
                                lowLatency = lowLatency,
                                onLowLatencyChange = {
                                    lowLatency = it
                                    viewModel.settings.lowLatencyMode = it
                                },
                                haptics = haptics,
                                onHapticsChange = {
                                    haptics = it
                                    viewModel.setHapticsEnabled(it)
                                },
                                diagnostics = diagnostics,
                                onDiagnosticsChange = {
                                    diagnostics = it
                                    viewModel.uiSettings.diagnosticsEnabled = it
                                },
                            )
                            UpdateSettingsCard(
                                viewModel = viewModel,
                                state = updateState,
                                autoUpdates = autoUpdates,
                                onAutoUpdatesChange = {
                                    autoUpdates = it
                                    viewModel.setAutomaticUpdates(it)
                                },
                                wifiOnly = wifiOnly,
                                onWifiOnlyChange = {
                                    wifiOnly = it
                                    viewModel.uiSettings.updateWifiOnly = it
                                },
                            )
                        }
                    }
                } else {
                    SteeringTuningCard(
                        viewModel = viewModel,
                        steeringMode = steeringMode,
                        onSteeringModeChange = {
                            steeringMode = it
                            viewModel.settings.steeringMode = it
                        },
                        steeringRange = steeringRange,
                        onSteeringRangeChange = {
                            steeringRange = it
                            viewModel.settings.steeringRange = it
                        },
                        tiltSensitivity = tiltSensitivity,
                        onTiltSensitivityChange = {
                            tiltSensitivity = it
                            viewModel.settings.tiltSensitivity = it
                        },
                        returnMode = returnMode,
                        onReturnModeChange = {
                            returnMode = it
                            viewModel.settings.touchReturnMode = it
                        },
                        wheelSide = wheelSide,
                        onWheelSideChange = {
                            wheelSide = it
                            viewModel.uiSettings.touchWheelSide = it
                        },
                        deadzone = deadzone,
                        onDeadzoneChange = {
                            deadzone = it
                            viewModel.uiSettings.steeringDeadzone = it
                        },
                        response = response,
                        onResponseChange = {
                            response = it
                            viewModel.uiSettings.steeringResponse = it
                        },
                        invertSteering = invertSteering,
                        onInvertChange = {
                            invertSteering = it
                            viewModel.uiSettings.invertSteering = it
                        },
                        compact = short,
                    )
                    DriveControlsCard(
                        viewModel = viewModel,
                        pedalMode = pedalMode,
                        onPedalModeChange = {
                            pedalMode = it
                            viewModel.uiSettings.pedalControlMode = it
                        },
                        lowLatency = lowLatency,
                        onLowLatencyChange = {
                            lowLatency = it
                            viewModel.settings.lowLatencyMode = it
                        },
                        haptics = haptics,
                        onHapticsChange = {
                            haptics = it
                            viewModel.setHapticsEnabled(it)
                        },
                        diagnostics = diagnostics,
                        onDiagnosticsChange = {
                            diagnostics = it
                            viewModel.uiSettings.diagnosticsEnabled = it
                        },
                    )
                    UpdateSettingsCard(
                        viewModel = viewModel,
                        state = updateState,
                        autoUpdates = autoUpdates,
                        onAutoUpdatesChange = {
                            autoUpdates = it
                            viewModel.setAutomaticUpdates(it)
                        },
                        wifiOnly = wifiOnly,
                        onWifiOnlyChange = {
                            wifiOnly = it
                            viewModel.uiSettings.updateWifiOnly = it
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SteeringTuningCard(
    viewModel: ControllerViewModel,
    steeringMode: SteeringMode,
    onSteeringModeChange: (SteeringMode) -> Unit,
    steeringRange: Int,
    onSteeringRangeChange: (Int) -> Unit,
    tiltSensitivity: Float,
    onTiltSensitivityChange: (Float) -> Unit,
    returnMode: ReturnMode,
    onReturnModeChange: (ReturnMode) -> Unit,
    wheelSide: TouchWheelSide,
    onWheelSideChange: (TouchWheelSide) -> Unit,
    deadzone: Float,
    onDeadzoneChange: (Float) -> Unit,
    response: Float,
    onResponseChange: (Float) -> Unit,
    invertSteering: Boolean,
    onInvertChange: (Boolean) -> Unit,
    compact: Boolean,
) {
    AdaptiveCard("Steering", "Tune the physical input before it reaches the 36-byte controller packet.") {
        SegmentedChips(
            labels = SteeringMode.entries.map { steeringModeLabel(it) },
            selectedIndex = SteeringMode.entries.indexOf(steeringMode),
            onSelected = { index ->
                onSteeringModeChange(SteeringMode.entries[index])
                if (viewModel.settings.hapticsEnabled) viewModel.haptics.modeChange()
            },
        )

        Spacer(Modifier.height(12.dp))
        SettingHeader("Range", "$steeringRange°")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            listOf(360, 540, 720, 900, 1080).forEach { preset ->
                FilterChip(
                    selected = steeringRange == preset,
                    onClick = { onSteeringRangeChange(preset) },
                    label = { Text("$preset°", maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Slider(
            value = steeringRange.toFloat(),
            onValueChange = { value ->
                onSteeringRangeChange(((value / 90f).roundToInt() * 90).coerceIn(180, 1080))
            },
            valueRange = 180f..1080f,
            steps = 9,
        )

        SettingHeader("Center deadzone", "${(deadzone * 100f).roundToInt()}%")
        Slider(value = deadzone, onValueChange = onDeadzoneChange, valueRange = 0f..0.12f)

        SettingHeader(
            "Response curve",
            when {
                response < 0.9f -> "Quick ${"%.2f".format(response)}x"
                response > 1.1f -> "Smooth ${"%.2f".format(response)}x"
                else -> "Linear"
            },
        )
        Slider(value = response, onValueChange = onResponseChange, valueRange = 0.55f..2f)

        ToggleLine(
            title = "Invert steering",
            description = "Flip left and right after deadzone and response shaping.",
            checked = invertSteering,
            onCheckedChange = onInvertChange,
        )

        if (steeringMode == SteeringMode.TILT) {
            HorizontalDivider(color = DriveBorder.copy(alpha = 0.6f))
            SettingHeader("Tilt sensitivity", "${"%.1f".format(tiltSensitivity)}x")
            Slider(value = tiltSensitivity, onValueChange = onTiltSensitivityChange, valueRange = 0.5f..3f)
        }

        if (steeringMode == SteeringMode.TOUCH) {
            HorizontalDivider(color = DriveBorder.copy(alpha = 0.6f))
            Text("Wheel side", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            SegmentedChips(
                labels = listOf("Left", "Right"),
                selectedIndex = if (wheelSide == TouchWheelSide.LEFT) 0 else 1,
                onSelected = { onWheelSideChange(if (it == 0) TouchWheelSide.LEFT else TouchWheelSide.RIGHT) },
            )
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            Text("Return behavior", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            SegmentedChips(
                labels = listOf("Smooth", "Instant", "Hold"),
                selectedIndex = ReturnMode.entries.indexOf(returnMode),
                onSelected = { onReturnModeChange(ReturnMode.entries[it]) },
            )
        }
    }
}

@Composable
private fun DriveControlsCard(
    viewModel: ControllerViewModel,
    pedalMode: PedalControlMode,
    onPedalModeChange: (PedalControlMode) -> Unit,
    lowLatency: Boolean,
    onLowLatencyChange: (Boolean) -> Unit,
    haptics: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    diagnostics: Boolean,
    onDiagnosticsChange: (Boolean) -> Unit,
) {
    AdaptiveCard("Driving & performance", "Keep large controls simple; expose detail only when you need it.") {
        Text("Pedals", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        SegmentedChips(
            labels = listOf("Arcade tap", "Analog slide"),
            selectedIndex = if (pedalMode == PedalControlMode.ARCADE) 0 else 1,
            onSelected = {
                onPedalModeChange(if (it == 0) PedalControlMode.ARCADE else PedalControlMode.ANALOG)
                if (haptics) viewModel.haptics.modeChange()
            },
        )
        Spacer(Modifier.height(10.dp))
        ToggleLine(
            title = "Adaptive low latency",
            description = "100 Hz heartbeat with faster sends while input changes.",
            checked = lowLatency,
            onCheckedChange = onLowLatencyChange,
            icon = { Icon(Icons.Rounded.Bolt, null, tint = DriveAccent) },
        )
        ToggleLine(
            title = "Haptic feedback",
            description = "Shift, handbrake and recenter confirmation.",
            checked = haptics,
            onCheckedChange = onHapticsChange,
            icon = { Icon(Icons.Rounded.Vibration, null, tint = DriveAccent) },
        )
        ToggleLine(
            title = "Live diagnostics",
            description = "Show sensor rate and worst packet gap while driving.",
            checked = diagnostics,
            onCheckedChange = onDiagnosticsChange,
            icon = { Icon(Icons.Rounded.Speed, null, tint = DriveAccent) },
        )
    }
}

@Composable
private fun UpdateSettingsCard(
    viewModel: ControllerViewModel,
    state: AppUpdateState,
    autoUpdates: Boolean,
    onAutoUpdatesChange: (Boolean) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
) {
    AdaptiveCard("App updates", "Checks the signed GitHub preview channel and verifies the APK before Android opens installation.") {
        ToggleLine(
            title = "Automatic update download",
            description = "Check on app start and prepare a verified APK in the background.",
            checked = autoUpdates,
            onCheckedChange = onAutoUpdatesChange,
            icon = { Icon(Icons.Rounded.Refresh, null, tint = DriveAccent) },
        )
        ToggleLine(
            title = "Wi-Fi only",
            description = "Avoid automatic APK downloads on metered mobile data.",
            checked = wifiOnly,
            onCheckedChange = onWifiOnlyChange,
            enabled = autoUpdates,
            icon = { Icon(Icons.Rounded.Wifi, null, tint = DriveAccent) },
        )

        Spacer(Modifier.height(6.dp))
        Surface(
            color = updateStateColor(state).copy(alpha = 0.10f),
            border = BorderStroke(1.dp, updateStateColor(state).copy(alpha = 0.34f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state is AppUpdateState.Checking || state is AppUpdateState.Downloading || state is AppUpdateState.Installing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, null, tint = updateStateColor(state), modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(updateTitle(state), color = DriveText, style = MaterialTheme.typography.titleSmall)
                    Text(
                        updateDetail(state),
                        color = DriveTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        when (state) {
            is AppUpdateState.Available -> Button(
                onClick = { viewModel.downloadAvailableUpdate(ignoreWifiPolicy = true) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DriveAccent, contentColor = DriveBackground),
            ) { Text("Download ${state.info.versionName}", fontWeight = FontWeight.Bold) }

            is AppUpdateState.Ready -> Button(
                onClick = viewModel::installReadyUpdate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DriveAccent, contentColor = DriveBackground),
            ) { Text("Install ${state.info.versionName}", fontWeight = FontWeight.Bold) }

            is AppUpdateState.PermissionRequired -> Button(
                onClick = viewModel::openUpdateInstallPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DriveWarning, contentColor = DriveBackground),
            ) { Text("Allow app updates", fontWeight = FontWeight.Bold) }

            is AppUpdateState.Downloading -> Text(
                "Download ${state.progress}%",
                color = DriveAccent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            else -> OutlinedButton(
                onClick = { viewModel.checkForUpdates(force = true) },
                enabled = state !is AppUpdateState.Checking && state !is AppUpdateState.Installing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state is AppUpdateState.Failed) "Retry update check" else "Check now") }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Android may still require one final install confirmation. Silent unattended installation is not assumed.",
            color = DriveTextFaint,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AdaptiveCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DriveSurface,
        border = BorderStroke(1.dp, DriveBorder),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SegmentedChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                label = {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingHeader(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        Text(value, color = DriveAccent, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ToggleLine(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Surface(
                color = DriveSurfaceRaised,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) DriveText else DriveTextFaint, style = MaterialTheme.typography.titleSmall)
            Text(description, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun updateStateColor(state: AppUpdateState) = when (state) {
    AppUpdateState.UpToDate -> DriveSuccess
    is AppUpdateState.Available, is AppUpdateState.Ready -> DriveAccent
    is AppUpdateState.PermissionRequired -> DriveWarning
    is AppUpdateState.Failed -> DriveDanger
    else -> DriveTextMuted
}

private fun updateTitle(state: AppUpdateState): String = when (state) {
    AppUpdateState.Idle -> "Update service ready"
    AppUpdateState.Checking -> "Checking for updates"
    AppUpdateState.UpToDate -> "You're up to date"
    is AppUpdateState.Available -> "${state.info.versionName} available"
    is AppUpdateState.Downloading -> "Downloading ${state.info.versionName}"
    is AppUpdateState.Ready -> "${state.info.versionName} verified"
    is AppUpdateState.PermissionRequired -> "Install permission required"
    is AppUpdateState.Installing -> "Opening Android installer"
    is AppUpdateState.Failed -> "Update check failed"
}

private fun updateDetail(state: AppUpdateState): String = when (state) {
    AppUpdateState.Idle -> "Signed GitHub preview channel"
    AppUpdateState.Checking -> "Reading release metadata"
    AppUpdateState.UpToDate -> "No newer signed preview is available"
    is AppUpdateState.Available -> "${state.info.sizeBytes.coerceAtLeast(0L) / 1_000_000L} MB · tap to download"
    is AppUpdateState.Downloading -> "${state.progress}% · SHA-256 and signer will be verified"
    is AppUpdateState.Ready -> "APK hash, package identity and signing certificate matched"
    is AppUpdateState.PermissionRequired -> "Allow PC Wheel to request APK installation once"
    is AppUpdateState.Installing -> "Android controls the final install confirmation"
    is AppUpdateState.Failed -> state.message
}
