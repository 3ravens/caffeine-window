package com.caffeinewindow.app

import android.app.Application
import android.util.Log
import com.caffeinewindow.app.repository.SettingsRepository
import com.caffeinewindow.app.service.WakeMonitorService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class CaffeineApplication : Application() {
  @Inject lateinit var settingsRepository: SettingsRepository

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  override fun onCreate() {
    super.onCreate()
    Log.d(CaffeineLog.TAG, "CaffeineApplication.onCreate starting WakeMonitorService")
    applicationScope.launch {
      settingsRepository.ensureDefaults()
    }
    WakeMonitorService.start(this)
  }
}
