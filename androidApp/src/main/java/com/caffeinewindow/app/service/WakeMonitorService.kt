package com.caffeinewindow.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.caffeinewindow.app.CaffeineLog
import com.caffeinewindow.app.MainActivity
import com.caffeinewindow.app.R
import com.caffeinewindow.app.notification.NotificationChannels
import com.caffeinewindow.app.repository.WakeRepository
import com.caffeinewindow.app.signal.ScreenUnlockSignalSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WakeMonitorService : Service() {
  @Inject lateinit var screenUnlockSignalSource: ScreenUnlockSignalSource
  @Inject lateinit var wakeRepository: WakeRepository

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var unlockReceiver: UnlockReceiver? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Log.d(CaffeineLog.TAG, "WakeMonitorService.onCreate")
    createMonitorChannel()
    startForegroundWithType(buildMonitorNotification())

    unlockReceiver = UnlockReceiver(screenUnlockSignalSource).also { receiver ->
      UnlockReceiver.register(this, receiver)
      Log.d(CaffeineLog.TAG, "UnlockReceiver registered for ACTION_USER_PRESENT")
    }

    screenUnlockSignalSource.observeWakeEvents()
      .onEach { event ->
        Log.d(CaffeineLog.TAG, "WakeMonitorService collected wake event at ${event.timestamp}")
        serviceScope.launch {
          val recorded = wakeRepository.recordWakeEvent(event)
          Log.d(
            CaffeineLog.TAG,
            if (recorded != null) {
              "WakeRepository recorded new wake event at ${recorded.timestamp}"
            } else {
              "WakeRepository ignored event (already recorded for this logical day)"
            },
          )
        }
      }
      .launchIn(serviceScope)

    // AlarmManager alarms die across reboot; restore today's schedule whenever the
    // monitor starts (boot via BootReceiver, sticky restart, or app launch).
    serviceScope.launch {
      wakeRepository.rescheduleFromLatestWake()
    }
  }

  override fun onDestroy() {
    unlockReceiver?.let { unregisterReceiver(it) }
    unlockReceiver = null
    serviceScope.cancel()
    super.onDestroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  private fun createMonitorChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
      NotificationChannels.MONITOR,
      getString(R.string.monitor_channel_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = getString(R.string.monitor_channel_description)
      setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
  }

  private fun buildMonitorNotification(): Notification {
    val launchIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Builder(this, NotificationChannels.MONITOR)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(getString(R.string.monitor_notification_title))
      .setContentText(getString(R.string.monitor_notification_body))
      .setContentIntent(launchIntent)
      .setOngoing(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  private fun startForegroundWithType(notification: Notification) {
    // The specialUse type only exists from API 34; older releases take no type.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        MONITOR_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(MONITOR_NOTIFICATION_ID, notification)
    }
  }

  companion object {
    private const val MONITOR_NOTIFICATION_ID = 1001

    /**
     * Safe to call from any process start. Android 12+ refuses foreground-service starts
     * from the background unless an exemption applies (boot, exact alarm, battery
     * exemption, visible activity); a refused start is logged instead of crashing the
     * process, so e.g. an inexact alarm can still post its notification.
     */
    fun start(context: Context) {
      val intent = Intent(context, WakeMonitorService::class.java)
      try {
        context.startForegroundService(intent)
      } catch (e: IllegalStateException) {
        Log.w(CaffeineLog.TAG, "WakeMonitorService start refused while in background", e)
      }
    }
  }
}
