package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DriveAccent
import com.example.ui.theme.DriveAccentMuted
import com.example.ui.theme.DriveBackground
import com.example.ui.theme.DriveBorder
import com.example.ui.theme.DriveSurface
import com.example.ui.theme.DriveSurfaceRaised
import com.example.ui.theme.DriveText
import com.example.ui.theme.DriveTextMuted
import kotlinx.coroutines.launch

@Composable
fun LandscapeConnectionScreen(
    viewModel: ControllerViewModel,
    onNavigateToController: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var ip by rememberSaveable { mutableStateOf(viewModel.settings.ip) }
    var port by rememberSaveable { mutableStateOf(viewModel.settings.port.toString()) }
    var discovered by remember { mutableStateOf<List<DiscoveredReceiver>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    val discoveryClient = remember { ReceiverDiscoveryClient() }
    val scope = rememberCoroutineScope()

    val parsedPort = port.toIntOrNull()
    val valid = ip.trim().isNotEmpty() && parsedPort != null && parsedPort in 1..65535

    fun applyReceiver(receiver: DiscoveredReceiver) {
        ip = receiver.ip
        port = receiver.port.toString()
    }

    fun startScan() {
        if (scanning) return
        scope.launch {
            scanning = true
            val found = runCatching { discoveryClient.discover() }.getOrDefault(emptyList())
            discovered = found
            scanCompleted = true
            if (found.size == 1) applyReceiver(found.first())
            scanning = false
        }
    }

    LaunchedEffect(Unit) {
        scanning = true
        val found = runCatching { discoveryClient.discover() }.getOrDefault(emptyList())
        discovered = found
        scanCompleted = true
        if (found.size == 1) applyReceiver(found.first())
        scanning = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DriveBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val compactHeight = maxHeight < 440.dp
        val outerHorizontal = if (compactHeight) 16.dp else 28.dp
        val outerVertical = if (compactHeight) 10.dp else 22.dp
        val gap = if (compactHeight) 16.dp else 28.dp
        val cardPadding = if (compactHeight) 14.dp else 24.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = outerHorizontal, vertical = outerVertical),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = DriveAccentMuted,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = if (compactHeight) 5.dp else 7.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = DriveAccent,
                            modifier = Modifier.size(if (compactHeight) 15.dp else 17.dp),
                        )
                        Text(
                            text = "PC WHEEL",
                            color = DriveAccent,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                Spacer(Modifier.height(if (compactHeight) 10.dp else 18.dp))
                Text(
                    text = "Drive with your phone.",
                    color = DriveText,
                    style = if (compactHeight) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.displaySmall
                    },
                )

                if (!compactHeight) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Motion steering, instant pedals and a latency-first local controller path.",
                        color = DriveTextMuted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.widthIn(max = 460.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LandscapeFeatureChip(
                            icon = { Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(15.dp)) },
                            text = if (viewModel.settings.lowLatencyMode) {
                                "Adaptive low latency"
                            } else {
                                "100 Hz balanced"
                            },
                        )
                        LandscapeFeatureChip(
                            icon = { Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(15.dp)) },
                            text = "${viewModel.settings.steeringRange}° range",
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.settings.lowLatencyMode) {
                            "Adaptive low latency · ${viewModel.settings.steeringRange}°"
                        } else {
                            "100 Hz balanced · ${viewModel.settings.steeringRange}°"
                        },
                        color = DriveTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .widthIn(max = 520.dp),
                color = DriveSurface,
                shape = RoundedCornerShape(if (compactHeight) 22.dp else 28.dp),
                border = BorderStroke(1.dp, DriveBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(cardPadding),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connect to PC",
                                color = DriveText,
                                style = if (compactHeight) {
                                    MaterialTheme.typography.titleLarge
                                } else {
                                    MaterialTheme.typography.headlineMedium
                                },
                            )
                            if (!compactHeight) {
                                Text(
                                    text = "Receiver discovery works automatically on the same local network.",
                                    color = DriveTextMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = DriveTextMuted,
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compactHeight) 6.dp else 10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        OutlinedButton(
                            onClick = ::startScan,
                            enabled = !scanning,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            if (scanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Finding receiver…")
                            } else {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(if (discovered.isEmpty()) "Find receiver" else "Scan again")
                            }
                        }

                        if (discovered.isNotEmpty()) {
                            Spacer(Modifier.height(7.dp))
                            discovered.take(if (compactHeight) 2 else 4).forEach { receiver ->
                                val selected = ip.trim() == receiver.ip && parsedPort == receiver.port
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clickable { applyReceiver(receiver) },
                                    color = if (selected) DriveAccentMuted else DriveSurfaceRaised,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) DriveAccent.copy(alpha = 0.55f) else DriveBorder,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Computer,
                                            null,
                                            tint = if (selected) DriveAccent else DriveTextMuted,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                receiver.name,
                                                color = DriveText,
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                            Text(
                                                "${receiver.ip}:${receiver.port}",
                                                color = DriveTextMuted,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                        if (selected) {
                                            Text(
                                                "SELECTED",
                                                color = DriveAccent,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (scanCompleted && !scanning) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "No receiver found automatically. Manual IP still works.",
                                color = DriveTextMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        Spacer(Modifier.height(if (compactHeight) 6.dp else 9.dp))
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("PC IPv4 address") },
                            leadingIcon = { Icon(Icons.Rounded.Computer, null) },
                        )
                        Spacer(Modifier.height(if (compactHeight) 6.dp else 10.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = { value ->
                                if (value.length <= 5 && value.all { it.isDigit() }) port = value
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("UDP port") },
                            supportingText = if (compactHeight) null else {
                                { Text("Receiver default: 26760") }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Rounded.Wifi, null) },
                            isError = port.isNotEmpty() && (parsedPort == null || parsedPort !in 1..65535),
                        )
                    }

                    Spacer(Modifier.height(if (compactHeight) 8.dp else 12.dp))

                    Button(
                        onClick = {
                            viewModel.settings.ip = ip.trim()
                            viewModel.settings.port = parsedPort ?: 26760
                            onNavigateToController()
                        },
                        enabled = valid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compactHeight) 50.dp else 54.dp),
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

@Composable
private fun LandscapeFeatureChip(
    icon: @Composable () -> Unit,
    text: String,
) {
    Surface(
        color = DriveAccentMuted.copy(alpha = 0.7f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = text,
                color = DriveTextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
