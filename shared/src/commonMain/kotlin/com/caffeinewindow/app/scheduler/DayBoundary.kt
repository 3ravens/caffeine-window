package com.caffeinewindow.app.scheduler

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DayBoundary {
  /**
   * Returns the start instant of the logical day containing [instant].
   * A logical day runs from [resetHour] on one calendar date until [resetHour] the next day.
   */
  fun logicalDayStart(
    instant: Instant,
    resetHour: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
  ): Instant {
    require(resetHour in 0..23) { "resetHour must be 0-23" }
    val local = instant.toLocalDateTime(timeZone)
    val date = if (local.hour < resetHour) {
      local.date.minus(1, DateTimeUnit.DAY)
    } else {
      local.date
    }
    return LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, resetHour, 0)
      .toInstant(timeZone)
  }

  fun isSameLogicalDay(
    first: Instant,
    second: Instant,
    resetHour: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
  ): Boolean {
    return logicalDayStart(first, resetHour, timeZone) == logicalDayStart(second, resetHour, timeZone)
  }
}
