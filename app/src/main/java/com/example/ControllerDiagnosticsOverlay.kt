package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DriveAccent
import com.example.ui.theme.DriveBorder
import com.example.ui.theme.DriveSurface
import com.example.ui.theme.DriveTextMuted

@Composable
fun ControllerDiagnosticsOverlay(
    viewModel: ControllerViewModel,
    modifier: Modifier = Modifier,
) {
    if (!viewModel.uiSettings.diagnosticsEnabled) return

    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    if (!isConnected) return

    val sensorRate by viewModel.sensorRateHz.collectAsStateWithLifecycle()
    val packetRate by viewModel.packetRate.collectAsStateWithLifecycle()
    val maxGapMicros by viewModel.maxPacketGapMicros.collectAsStateWithLifecycle()
    val latency by viewModel.latency.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier,
        color = DriveSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DriveBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Metric("SENSOR", if (sensorRate > 0) "$sensorRate Hz" else "—")
            Metric("TX", if (packetRate > 0) "$packetRate Hz" else "—")
            Metric("GAP", if (maxGapMicros > 0L) "${maxGapMicros / 1000.0} ms" else "—")
            Metric("RTT", if (latency > 0L) "$latency ms" else "—")
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Text(
        "$label $value",
        color = if (value == "—") DriveTextMuted else DriveAccent,
        style = MaterialTheme.typography.labelSmall,
    )
}
