package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(viewModel: ControllerViewModel, onNavigateToController: () -> Unit, onNavigateToSettings: () -> Unit) {
    var ip by remember { mutableStateOf(viewModel.settings.ip) }
    var port by remember { mutableStateOf(viewModel.settings.port.toString()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PC Wheel - Connection") }, actions = {
            Button(onClick = onNavigateToSettings) { Text("Settings") }
        }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("PC IP Address") },
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("UDP Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.settings.ip = ip
                    port.toIntOrNull()?.let { viewModel.settings.port = it }
                    onNavigateToController()
                },
                modifier = Modifier.width(200.dp).height(56.dp)
            ) {
                Text("CONNECT & PLAY", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ControllerViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(viewModel.settings.steeringMode) }
    var range by remember { mutableStateOf(viewModel.settings.steeringRange) }
    var sensitivity by remember { mutableStateOf(viewModel.settings.tiltSensitivity) }
    var returnMode by remember { mutableStateOf(viewModel.settings.touchReturnMode) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = {
            Button(onClick = onBack, modifier = Modifier.padding(8.dp)) { Text("Back") }
        }) }
    ) { padding ->
        Row(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Steering Mode", fontWeight = FontWeight.Bold)
                SteeringMode.values().forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == m, onClick = { 
                            mode = m
                            viewModel.settings.steeringMode = m 
                        })
                        Text(m.name)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (mode == SteeringMode.TOUCH) {
                    Text("Touch Return Mode", fontWeight = FontWeight.Bold)
                    ReturnMode.values().forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = returnMode == m, onClick = { 
                                returnMode = m
                                viewModel.settings.touchReturnMode = m 
                            })
                            Text(m.name)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text("Steering Range: $range°", fontWeight = FontWeight.Bold)
                Slider(
                    value = range.toFloat(),
                    onValueChange = { 
                        range = it.toInt()
                        viewModel.settings.steeringRange = it.toInt() 
                    },
                    valueRange = 180f..1080f,
                    steps = (1080 - 180) / 90 - 1
                )
                
                if (mode == SteeringMode.TILT) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tilt Sensitivity: ${"%.1f".format(sensitivity)}x", fontWeight = FontWeight.Bold)
                    Slider(
                        value = sensitivity,
                        onValueChange = { 
                            sensitivity = it
                            viewModel.settings.tiltSensitivity = it 
                        },
                        valueRange = 0.5f..3.0f
                    )
                }
            }
        }
    }
}

@Composable
fun ControllerScreen(viewModel: ControllerViewModel, onDisconnect: () -> Unit) {
    val state by viewModel.controllerState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val latency by viewModel.latency.collectAsStateWithLifecycle()
    val packetRate by viewModel.packetRate.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startController()
        onDispose { viewModel.stopController() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).windowInsetsPadding(WindowInsets.safeDrawing)) {
        // HUD at the top
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = if (isConnected) Color(0xFF00E676) else Color(0xFFFF1744)
            val statusText = if (isConnected) "● CONNECTED  ${latency}ms  $packetRate Hz" else "● CONNECTING..."
            
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            Text(viewModel.settings.steeringMode.name, color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
            
            Text("STEERING ${if (state.steering > 0) "+" else ""}${(state.steering * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            Button(onClick = onDisconnect, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) {
                Text("⚙", fontSize = 20.sp)
            }
        }

        // Center Steering Area
        Box(modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.7f).fillMaxWidth(0.4f), contentAlignment = Alignment.Center) {
            when (viewModel.settings.steeringMode) {
                SteeringMode.TOUCH -> {
                    TouchWheel(
                        currentAngle = state.steering * (viewModel.settings.steeringRange / 2f),
                        onAngleDelta = { viewModel.handleTouchWheelDelta(it) },
                        onRelease = { viewModel.handleTouchWheelRelease() },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
                SteeringMode.MOTION, SteeringMode.TILT -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(state.steering * (viewModel.settings.steeringRange / 2f)).toInt()}°",
                            color = Color.White,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (viewModel.settings.steeringMode == SteeringMode.MOTION && !viewModel.sensorHandler.hasRotationSensor) {
                            Text("Warning: Rotation Vector sensor not available.", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Left Analog Controls (Clutch & Throttle)
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp, top = 48.dp, bottom = 100.dp).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnalogSlider(value = state.clutch, onValueChange = { viewModel.updateAnalog(AnalogInput.CLUTCH, it) }, color = Color(0xFF1E88E5), label = "CLUTCH")
            AnalogSlider(value = state.throttle, onValueChange = { viewModel.updateAnalog(AnalogInput.THROTTLE, it) }, color = Color(0xFF43A047), label = "GAS")
        }

        // Right Analog Controls (Brake & Handbrake)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp, top = 48.dp, bottom = 100.dp).fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnalogSlider(value = state.brake, onValueChange = { viewModel.updateAnalog(AnalogInput.BRAKE, it) }, color = Color(0xFFE53935), label = "BRAKE")
            AnalogSlider(value = state.handbrake, onValueChange = { viewModel.updateAnalog(AnalogInput.HANDBRAKE, it) }, color = Color(0xFFFDD835), label = "E-BRAKE")
        }

        // Bottom Action Buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControllerButton("SHIFT -", { viewModel.updateButton(ButtonInput.SHIFT_DOWN, true) }, { viewModel.updateButton(ButtonInput.SHIFT_DOWN, false) }, modifier = Modifier.size(90.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ControllerButton("HORN", { viewModel.updateButton(ButtonInput.HORN, true) }, { viewModel.updateButton(ButtonInput.HORN, false) })
                ControllerButton("CENTER", { 
                    viewModel.updateButton(ButtonInput.RESET, true)
                    viewModel.calibrate()
                }, { viewModel.updateButton(ButtonInput.RESET, false) })
                ControllerButton("CAM", { viewModel.updateButton(ButtonInput.CAMERA, true) }, { viewModel.updateButton(ButtonInput.CAMERA, false) })
            }
            
            ControllerButton("SHIFT +", { viewModel.updateButton(ButtonInput.SHIFT_UP, true) }, { viewModel.updateButton(ButtonInput.SHIFT_UP, false) }, modifier = Modifier.size(90.dp))
        }
    }
}
