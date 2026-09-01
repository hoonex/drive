package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DriveColorScheme = darkColorScheme(
  primary = DriveAccent,
  onPrimary = DriveBackground,
  primaryContainer = DriveAccentMuted,
  onPrimaryContainer = DriveText,
  secondary = DriveAccentStrong,
  onSecondary = DriveBackground,
  background = DriveBackground,
  onBackground = DriveText,
  surface = DriveSurface,
  onSurface = DriveText,
  surfaceVariant = DriveSurfaceRaised,
  onSurfaceVariant = DriveTextMuted,
  outline = DriveBorder,
  error = DriveDanger,
  onError = DriveBackground,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = DriveColorScheme,
    typography = Typography,
    content = content,
  )
}
