package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ControllerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    hideSystemUi()

    setContent {
      MyApplicationTheme {
        var currentScreen by remember { mutableStateOf("connection") }

        when (currentScreen) {
          "connection" -> LandscapeConnectionScreen(
            viewModel = viewModel,
            onNavigateToController = { currentScreen = "controller" },
            onNavigateToSettings = { currentScreen = "settings" }
          )
          "settings" -> ProductionSettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "connection" }
          )
          "controller" -> ProductionControllerScreen(
            viewModel = viewModel,
            onDisconnect = { currentScreen = "connection" }
          )
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.resumeControllerIfRequested()
  }

  override fun onStop() {
    viewModel.pauseControllerForBackground()
    super.onStop()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemUi()
  }

  private fun hideSystemUi() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
      hide(WindowInsetsCompat.Type.systemBars())
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }
}
