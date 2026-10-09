package com.caffeinewindow.app.permission

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.caffeinewindow.app.notification.NotificationChannels

data class PermissionStatus(
  val notificationsGranted: Boolean,
  val exactAlarmsGranted: Boolean,
  val batteryOptimizationDisabled: Boolean,
) {
  val allReady: Boolean
    get() = notificationsGranted && exactAlarmsGranted && batteryOptimizationDisabled
}

object PermissionHelper {
  fun getStatus(context: Context): PermissionStatus {
    return PermissionStatus(
      notificationsGranted = areNotificationsGranted(context),
      exactAlarmsGranted = canScheduleExactAlarms(context),
      batteryOptimizationDisabled = isIgnoringBatteryOptimizations(context),
    )
  }

  fun areNotificationsGranted(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    }
    return true
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager.canScheduleExactAlarms()
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java)
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun createExactAlarmSettingsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
      data = Uri.parse("package:${context.packageName}")
    }
  }

  fun createBatteryOptimizationRequestIntent(context: Context): Intent {
    return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
      data = Uri.parse("package:${context.packageName}")
    }
  }

  fun createBatteryOptimizationSettingsIntent(): Intent {
    return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  }

  fun createAppNotificationSettingsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
      putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
  }

  fun createApplicationDetailsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.parse("package:${context.packageName}")
    }
  }

  fun isMonitorChannelMuted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val channel = context.getSystemService(NotificationManager::class.java)
      .getNotificationChannel(NotificationChannels.MONITOR)
      ?: return false
    return channel.importance == NotificationManager.IMPORTANCE_NONE
  }

  fun createMonitorChannelSettingsIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannels.MONITOR)
      }
    } else {
      createApplicationDetailsIntent(context)
    }
  }
}
