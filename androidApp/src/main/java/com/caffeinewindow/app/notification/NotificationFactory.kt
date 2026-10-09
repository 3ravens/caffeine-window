package com.caffeinewindow.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.caffeinewindow.app.MainActivity
import com.caffeinewindow.app.R

class NotificationFactory(
  private val context: Context,
) {
  init {
    createChannel()
  }

  fun showCaffeineWindowNotification(title: String, body: String) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val launchIntent = PendingIntent.getActivity(
      context,
      0,
      Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, NotificationChannels.CAFFEINE_ALERT)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setContentIntent(launchIntent)
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .build()
    manager.notify(NOTIFICATION_ID, notification)
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
      NotificationChannels.CAFFEINE_ALERT,
      context.getString(R.string.caffeine_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = context.getString(R.string.caffeine_channel_description)
    }
    manager.createNotificationChannel(channel)
  }

  companion object {
    const val NOTIFICATION_ID = 2001
  }
}
