package com.caffeinewindow.app.ui.format

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * How a past wake event relates to the current calendar date.
 * Relative to the calendar date, not the logical day: "yesterday" reads correctly in the
 * idle copy regardless of where the day-start hour falls.
 */
enum class RelativeDay { TODAY, YESTERDAY, OLDER }

data class WakeLabel(
    val day: RelativeDay,
    /** Local wall-clock time, e.g. "07:04". 24h until locale-aware formatting lands. */
    val clock: String,
    /** Localized date, only set for [RelativeDay.OLDER]. */
    val date: String?,
)

fun formatClockTime(instant: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = instant.toLocalDateTime(zone)
    return "%02d:%02d".format(local.hour, local.minute)
}

fun formatHourLabel(hour: Int): String = "$hour:00"

fun wakeLabel(
    instant: Instant,
    now: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): WakeLabel {
    val eventDate = instant.toLocalDateTime(zone).date
    val today = now.toLocalDateTime(zone).date
    val daysAgo = today.toEpochDays() - eventDate.toEpochDays()
    val day = when (daysAgo) {
        0 -> RelativeDay.TODAY
        1 -> RelativeDay.YESTERDAY
        else -> RelativeDay.OLDER
    }
    val date = if (day == RelativeDay.OLDER) {
        val localized = java.time.LocalDate.of(eventDate.year, eventDate.monthNumber, eventDate.dayOfMonth)
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(localized)
    } else {
        null
    }
    return WakeLabel(day = day, clock = formatClockTime(instant, zone), date = date)
}
