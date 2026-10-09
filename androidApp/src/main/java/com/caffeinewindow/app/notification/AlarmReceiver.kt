package com.caffeinewindow.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_SHOW_CAFFEINE_NOTIFICATION) return
    val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
    if (title.isBlank() || body.isBlank()) return
    NotificationFactory(context.applicationContext).showCaffeineWindowNotification(title, body)
  }

  companion object {
    const val ACTION_SHOW_CAFFEINE_NOTIFICATION =
      "com.caffeinewindow.app.action.SHOW_CAFFEINE_NOTIFICATION"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
  }
}
