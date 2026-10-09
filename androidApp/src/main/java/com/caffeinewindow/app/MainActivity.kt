package com.caffeinewindow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.caffeinewindow.app.service.WakeMonitorService
import com.caffeinewindow.app.ui.CaffeineWindowApp
import com.caffeinewindow.app.ui.theme.CaffeineWindowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CaffeineWindowTheme {
        CaffeineWindowApp()
      }
    }
  }

  override fun onStart() {
    super.onStart()
    // Re-assert the monitor in case the OS killed the service while the process stayed alive.
    WakeMonitorService.start(this)
  }
}
