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
import androidx.compose.ui.graphics.Color
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
    var mode by remember { mutableStateOf(viewModel.settings.steeringMode) }
    var range by remember { mutableIntStateOf(viewModel.settings.steeringRange) }
    var tilt by remember { mutableFloatStateOf(viewModel.settings.tiltSensitivity) }
    var returnMode by remember { mutableStateOf(viewModel.settings.touchReturnMode) }
    var pedalMode by remember { mutableStateOf(viewModel.uiSettings.pedalControlMode) }
    var wheelSide by remember { mutableStateOf(viewModel.uiSettings.touchWheelSide) }
    var deadzone by remember { mutableFloatStateOf(viewModel.uiSettings.steeringDeadzone) }
    var response by remember { mutableFloatStateOf(viewModel.uiSettings.steeringResponse) }
    var inverted by remember { mutableStateOf(viewModel.uiSettings.invertSteering) }
    var lowLatency by remember { mutableStateOf(viewModel.settings.lowLatencyMode) }
    var haptics by remember { mutableStateOf(viewModel.settings.hapticsEnabled) }
    var diagnostics by remember { mutableStateOf(viewModel.uiSettings.diagnosticsEnabled) }
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
        val wide = maxWidth >= 900.dp && maxHeight >= 400.dp
        val edge = if (short) 10.dp else 16.dp
        val gap = if (short) 8.dp else 12.dp

        Column(Modifier.fillMaxSize()) {
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
                Column(Modifier.weight(1f)) {
                    Text(
                        "Controller setup",
                        color = DriveText,
                        style = if (short) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    )
                    if (!short) {
                        Text(
                            "Steering feel, layout, diagnostics and updates",
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
                val steeringCard: @Composable () -> Unit = {
                    SteeringCard(
                        mode = mode,
                        onMode = {
                            mode = it
                            viewModel.settings.steeringMode = it
                            if (haptics) viewModel.haptics.modeChange()
                        },
                        range = range,
                        onRange = {
                            range = it
                            viewModel.settings.steeringRange = it
                        },
                        tilt = tilt,
                        onTilt = {
                            tilt = it
                            viewModel.settings.tiltSensitivity = it
                        },
                        returnMode = returnMode,
                        onReturnMode = {
                            returnMode = it
                            viewModel.settings.touchReturnMode = it
                        },
                        wheelSide = wheelSide,
                        onWheelSide = {
                            wheelSide = it
                            viewModel.uiSettings.touchWheelSide = it
                        },
                        deadzone = deadzone,
                        onDeadzone = {
                            deadzone = it
                            viewModel.uiSettings.steeringDeadzone = it
                        },
                        response = response,
                        onResponse = {
                            response = it
                            viewModel.uiSettings.steeringResponse = it
                        },
                        inverted = inverted,
                        onInverted = {
                            inverted = it
                            viewModel.uiSettings.invertSteering = it
                        },
                    )
                }

                val controlsCard: @Composable () -> Unit = {
                    ControlsCard(
                        pedalMode = pedalMode,
                        onPedalMode = {
                            pedalMode = it
                            viewModel.uiSettings.pedalControlMode = it
                            if (haptics) viewModel.haptics.modeChange()
                        },
                        lowLatency = lowLatency,
                        onLowLatency = {
                            lowLatency = it
                            viewModel.settings.lowLatencyMode = it
                        },
                        haptics = haptics,
                        onHaptics = {
                            haptics = it
                            viewModel.setHapticsEnabled(it)
                        },
                        diagnostics = diagnostics,
                        onDiagnostics = {
                            diagnostics = it
                            viewModel.uiSettings.diagnosticsEnabled = it
                        },
                    )
                }

                val updateCard: @Composable () -> Unit = {
                    UpdatesCard(
                        viewModel = viewModel,
                        state = updateState,
                        autoUpdates = autoUpdates,
                        onAutoUpdates = {
                            autoUpdates = it
                            viewModel.setAutomaticUpdates(it)
                        },
                        wifiOnly = wifiOnly,
                        onWifiOnly = {
                            wifiOnly = it
                            viewModel.uiSettings.updateWifiOnly = it
                        },
                    )
                }

                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1.06f),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) { steeringCard() }
                        Column(
                            modifier = Modifier.weight(0.94f),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            controlsCard()
                            updateCard()
                        }
                    }
                } else {
                    steeringCard()
                    controlsCard()
                    updateCard()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SteeringCard(
    mode: SteeringMode,
    onMode: (SteeringMode) -> Unit,
    range: Int,
    onRange: (Int) -> Unit,
    tilt: Float,
    onTilt: (Float) -> Unit,
    returnMode: ReturnMode,
    onReturnMode: (ReturnMode) -> Unit,
    wheelSide: TouchWheelSide,
    onWheelSide: (TouchWheelSide) -> Unit,
    deadzone: Float,
    onDeadzone: (Float) -> Unit,
    response: Float,
    onResponse: (Float) -> Unit,
    inverted: Boolean,
    onInverted: (Boolean) -> Unit,
) {
    SettingsCard("Steering", "Tune input feel before it reaches the PC packet.") {
        ChoiceRow(
            labels = SteeringMode.entries.map(::settingsModeLabel),
            selected = SteeringMode.entries.indexOf(mode),
            onSelected = { onMode(SteeringMode.entries[it]) },
        )

        Spacer(Modifier.height(10.dp))
        ValueHeader("Range", "$range°")
        ChoiceRow(
            labels = listOf("360°", "540°", "720°", "900°", "1080°"),
            selected = listOf(360, 540, 720, 900, 1080).indexOf(range),
            onSelected = { onRange(listOf(360, 540, 720, 900, 1080)[it]) },
        )
        Slider(
            value = range.toFloat(),
            onValueChange = { onRange(((it / 90f).roundToInt() * 90).coerceIn(180, 1080)) },
            valueRange = 180f..1080f,
            steps = 9,
        )

        ValueHeader("Center deadzone", "${(deadzone * 100f).roundToInt()}%")
        Slider(value = deadzone, onValueChange = onDeadzone, valueRange = 0f..0.12f)

        ValueHeader(
            "Response curve",
            when {
                response < 0.9f -> "Quick ${"%.2f".format(response)}x"
                response > 1.1f -> "Smooth ${"%.2f".format(response)}x"
                else -> "Linear"
            },
        )
        Slider(value = response, onValueChange = onResponse, valueRange = 0.55f..2f)

        SwitchRow(
            title = "Invert steering",
            description = "Flip left/right after deadzone and response shaping.",
            checked = inverted,
            onChecked = onInverted,
        )

        if (mode == SteeringMode.TILT) {
            HorizontalDivider(color = DriveBorder.copy(alpha = 0.6f))
            ValueHeader("Tilt sensitivity", "${"%.1f".format(tilt)}x")
            Slider(value = tilt, onValueChange = onTilt, valueRange = 0.5f..3f)
        }

        if (mode == SteeringMode.TOUCH) {
            HorizontalDivider(color = DriveBorder.copy(alpha = 0.6f))
            Text("Wheel side", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                labels = listOf("Left", "Right"),
                selected = if (wheelSide == TouchWheelSide.LEFT) 0 else 1,
                onSelected = { onWheelSide(if (it == 0) TouchWheelSide.LEFT else TouchWheelSide.RIGHT) },
            )
            Spacer(Modifier.height(7.dp))
            Text("Return behavior", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
            ChoiceRow(
                labels = listOf("Smooth", "Instant", "Hold"),
                selected = ReturnMode.entries.indexOf(returnMode),
                onSelected = { onReturnMode(ReturnMode.entries[it]) },
            )
        }
    }
}

@Composable
private fun ControlsCard(
    pedalMode: PedalControlMode,
    onPedalMode: (PedalControlMode) -> Unit,
    lowLatency: Boolean,
    onLowLatency: (Boolean) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    diagnostics: Boolean,
    onDiagnostics: (Boolean) -> Unit,
) {
    SettingsCard("Driving & performance", "Keep the driving screen uncluttered by default.") {
        Text("Pedals", color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        ChoiceRow(
            labels = listOf("Arcade tap", "Analog slide"),
            selected = if (pedalMode == PedalControlMode.ARCADE) 0 else 1,
            onSelected = { onPedalMode(if (it == 0) PedalControlMode.ARCADE else PedalControlMode.ANALOG) },
        )
        Spacer(Modifier.height(8.dp))
        SwitchRow(
            "Adaptive low latency",
            "100 Hz heartbeat plus faster sends while input changes.",
            lowLatency,
            onLowLatency,
            { Icon(Icons.Rounded.Bolt, null, tint = DriveAccent) },
        )
        SwitchRow(
            "Haptic feedback",
            "Shift, handbrake and recenter confirmation.",
            haptics,
            onHaptics,
            { Icon(Icons.Rounded.Vibration, null, tint = DriveAccent) },
        )
        SwitchRow(
            "Live diagnostics",
            "Optional sensor Hz, TX Hz, max packet gap and RTT overlay.",
            diagnostics,
            onDiagnostics,
            { Icon(Icons.Rounded.Speed, null, tint = DriveAccent) },
        )
    }
}

@Composable
private fun UpdatesCard(
    viewModel: ControllerViewModel,
    state: AppUpdateState,
    autoUpdates: Boolean,
    onAutoUpdates: (Boolean) -> Unit,
    wifiOnly: Boolean,
    onWifiOnly: (Boolean) -> Unit,
) {
    SettingsCard("App updates", "Signed GitHub preview updates with APK verification before install.") {
        SwitchRow(
            "Automatic download",
            "Check on startup and prepare newer verified builds automatically.",
            autoUpdates,
            onAutoUpdates,
            { Icon(Icons.Rounded.Refresh, null, tint = DriveAccent) },
        )
        SwitchRow(
            "Wi-Fi only",
            "Avoid automatic APK downloads on metered networks.",
            wifiOnly,
            onWifiOnly,
            { Icon(Icons.Rounded.Wifi, null, tint = DriveAccent) },
            enabled = autoUpdates,
        )

        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = updateColor(state).copy(alpha = 0.10f),
            border = BorderStroke(1.dp, updateColor(state).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(15.dp),
        ) {
            Row(
                modifier = Modifier.padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (state is AppUpdateState.Checking || state is AppUpdateState.Downloading || state is AppUpdateState.Installing) {
                    CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, null, tint = updateColor(state), modifier = Modifier.size(19.dp))
                }
                Column(Modifier.weight(1f)) {
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

        Spacer(Modifier.height(9.dp))
        when (state) {
            is AppUpdateState.Available -> PrimaryAction("Download ${state.info.versionName}") {
                viewModel.downloadAvailableUpdate(ignoreWifiPolicy = true)
            }
            is AppUpdateState.Ready -> PrimaryAction("Install ${state.info.versionName}", viewModel::installReadyUpdate)
            is AppUpdateState.PermissionRequired -> PrimaryAction(
                "Allow app updates",
                viewModel::openUpdateInstallPermission,
                DriveWarning,
            )
            is AppUpdateState.Downloading -> Text(
                "Download ${state.progress}%",
                color = DriveAccent,
                style = MaterialTheme.typography.labelLarge,
            )
            else -> OutlinedButton(
                onClick = { viewModel.checkForUpdates(force = true) },
                enabled = state !is AppUpdateState.Checking && state !is AppUpdateState.Installing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state is AppUpdateState.Failed) "Retry update check" else "Check now") }
        }

        Spacer(Modifier.height(5.dp))
        Text(
            "Android can still require a final install confirmation; silent installation is not assumed.",
            color = DriveTextFaint,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DriveSurface,
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = DriveText, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(11.dp))
            content()
        }
    }
}

@Composable
private fun ChoiceRow(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelected(index) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ValueHeader(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = DriveTextMuted, style = MaterialTheme.typography.labelLarge)
        Text(value, color = DriveAccent, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
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
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.size(35.dp),
            ) { Box(contentAlignment = Alignment.Center) { icon() } }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) DriveText else DriveTextFaint, style = MaterialTheme.typography.titleSmall)
            Text(description, color = DriveTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun PrimaryAction(label: String, action: () -> Unit, color: Color = DriveAccent) {
    Button(
        onClick = action,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = DriveBackground),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

private fun settingsModeLabel(mode: SteeringMode): String = when (mode) {
    SteeringMode.MOTION -> "Motion"
    SteeringMode.TILT -> "Tilt"
    SteeringMode.TOUCH -> "Touch"
}

private fun updateColor(state: AppUpdateState): Color = when (state) {
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
    AppUpdateState.Idle -> "Signed preview channel"
    AppUpdateState.Checking -> "Reading release metadata"
    AppUpdateState.UpToDate -> "No newer signed preview is available"
    is AppUpdateState.Available -> "${state.info.sizeBytes.coerceAtLeast(0L) / 1_000_000L} MB · ready to download"
    is AppUpdateState.Downloading -> "${state.progress}% · hash and signer verification follows"
    is AppUpdateState.Ready -> "APK hash, package and signing certificate matched"
    is AppUpdateState.PermissionRequired -> "Allow PC Wheel to request APK installation once"
    is AppUpdateState.Installing -> "Android controls the final confirmation"
    is AppUpdateState.Failed -> state.message
}
