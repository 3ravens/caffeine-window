package com.caffeinewindow.app.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant

class SchedulingMathTest {
  private val wake = Instant.parse("2025-06-25T06:00:00Z")

  @Test
  fun targetNotificationTimeAddsDelay() {
    val target = SchedulingMath.targetNotificationTime(wake, delayMinutes = 90)
    assertEquals(Instant.parse("2025-06-25T07:30:00Z"), target)
  }

  @Test
  fun isWindowOpenBeforeAndAfterTarget() {
    assertFalse(
      SchedulingMath.isWindowOpen(wake, 90, Instant.parse("2025-06-25T07:00:00Z")),
    )
    assertTrue(
      SchedulingMath.isWindowOpen(wake, 90, Instant.parse("2025-06-25T07:30:00Z")),
    )
  }

  @Test
  fun remainingDurationCountsDown() {
    val remaining = SchedulingMath.remainingDuration(
      wake,
      delayMinutes = 90,
      now = Instant.parse("2025-06-25T06:30:00Z"),
    )
    assertEquals(60.minutes, remaining)
  }
}
