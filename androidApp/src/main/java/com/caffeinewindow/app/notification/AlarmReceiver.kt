package com.caffeinewindow.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.caffeinewindow.app.repository.WakeRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class AlarmReceiver : BroadcastReceiver() {
  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface AlarmReceiverEntryPoint {
    fun wakeRepository(): WakeRepository
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_SHOW_CAFFEINE_NOTIFICATION) return
    val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
    if (title.isBlank() || body.isBlank()) return
    val appContext = context.applicationContext
    NotificationFactory(appContext).showCaffeineWindowNotification(title, body)

    // Record the shown day so a later reschedule (settings change, service restart,
    // reboot) does not show the same day's notification again.
    val logicalDayStartMillis = intent.getLongExtra(EXTRA_LOGICAL_DAY_START, 0L)
    if (logicalDayStartMillis == 0L) return
    val wakeRepository = EntryPointAccessors
      .fromApplication(appContext, AlarmReceiverEntryPoint::class.java)
      .wakeRepository()
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        wakeRepository.markCaffeineWindowNotified(
          Instant.fromEpochMilliseconds(logicalDayStartMillis),
        )
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_SHOW_CAFFEINE_NOTIFICATION =
      "com.caffeinewindow.app.action.SHOW_CAFFEINE_NOTIFICATION"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_LOGICAL_DAY_START = "extra_logical_day_start"
  }
}
