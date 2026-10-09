package com.caffeinewindow.app.scheduler

import kotlinx.datetime.Instant

interface NotificationScheduler {
  suspend fun scheduleCaffeineWindowNotification(
    wakeTimestamp: Instant,
    delayMinutes: Int,
    notificationTitle: String,
    notificationBody: String,
  )

  suspend fun cancelScheduledNotification()
}
