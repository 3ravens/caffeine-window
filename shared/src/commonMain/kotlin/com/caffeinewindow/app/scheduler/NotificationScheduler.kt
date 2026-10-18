package com.caffeinewindow.app.scheduler

import kotlinx.datetime.Instant

/**
 * Platform alarm + notification delivery. Deciding *whether* to schedule or show
 * (one notification per logical day) belongs to `WakeRepository`, not here.
 */
interface NotificationScheduler {
  /**
   * Replaces any pending caffeine-window alarm with one at [triggerAt]. When it fires,
   * the platform shows the notification and reports [logicalDayStart] back through
   * `WakeRepository.markCaffeineWindowNotified`.
   */
  suspend fun scheduleCaffeineWindowNotification(
    triggerAt: Instant,
    logicalDayStart: Instant,
    notificationTitle: String,
    notificationBody: String,
  )

  suspend fun showCaffeineWindowNotification(
    notificationTitle: String,
    notificationBody: String,
  )

  suspend fun cancelScheduledNotification()
}
