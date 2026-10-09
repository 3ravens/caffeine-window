package com.caffeinewindow.app.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class DayBoundaryTest {
  private val utc = TimeZone.UTC

  @Test
  fun unlockBeforeResetHourBelongsToPreviousLogicalDay() {
    val instant = Instant.parse("2025-06-25T01:30:00Z")
    val start = DayBoundary.logicalDayStart(instant, resetHour = 3, timeZone = utc)
    assertEquals(Instant.parse("2025-06-24T03:00:00Z"), start)
  }

  @Test
  fun unlockAfterResetHourBelongsToSameCalendarDay() {
    val instant = Instant.parse("2025-06-25T04:00:00Z")
    val start = DayBoundary.logicalDayStart(instant, resetHour = 3, timeZone = utc)
    assertEquals(Instant.parse("2025-06-25T03:00:00Z"), start)
  }

  @Test
  fun isSameLogicalDayGroupsEventsCorrectly() {
    val first = Instant.parse("2025-06-25T08:00:00Z")
    val second = Instant.parse("2025-06-25T22:00:00Z")
    val third = Instant.parse("2025-06-26T01:00:00Z")
    assertTrue(DayBoundary.isSameLogicalDay(first, second, resetHour = 3, timeZone = utc))
    assertTrue(DayBoundary.isSameLogicalDay(first, third, resetHour = 3, timeZone = utc))
    assertFalse(
      DayBoundary.isSameLogicalDay(
        first,
        Instant.parse("2025-06-26T04:00:00Z"),
        resetHour = 3,
        timeZone = utc,
      ),
    )
  }
}
