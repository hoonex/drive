package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ControllerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var currentScreen by remember { mutableStateOf("connection") }

        when (currentScreen) {
          "connection" -> ConnectionScreen(
            viewModel = viewModel,
            onNavigateToController = { currentScreen = "controller" },
            onNavigateToSettings = { currentScreen = "settings" }
          )
          "settings" -> SettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "connection" }
          )
          "controller" -> ControllerScreen(
            viewModel = viewModel,
            onDisconnect = { currentScreen = "connection" }
          )
        }
      }
    }
  }
}
