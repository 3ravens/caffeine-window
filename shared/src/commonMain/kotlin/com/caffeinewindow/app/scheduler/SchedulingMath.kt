package com.caffeinewindow.app.scheduler

import kotlin.time.Duration
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus

object SchedulingMath {
  fun targetNotificationTime(wakeTimestamp: Instant, delayMinutes: Int): Instant {
    require(delayMinutes > 0) { "delayMinutes must be positive" }
    return wakeTimestamp.plus(delayMinutes, DateTimeUnit.MINUTE)
  }

  fun isWindowOpen(wakeTimestamp: Instant, delayMinutes: Int, now: Instant): Boolean {
    return now >= targetNotificationTime(wakeTimestamp, delayMinutes)
  }

  fun remainingDuration(wakeTimestamp: Instant, delayMinutes: Int, now: Instant): Duration {
    val target = targetNotificationTime(wakeTimestamp, delayMinutes)
    return if (now >= target) {
      Duration.ZERO
    } else {
      (target - now).absoluteValue
    }
  }
}
