package com.caffeinewindow.app.repository

import com.caffeinewindow.app.db.WakeEventDao
import com.caffeinewindow.app.db.WakeEventEntity
import com.caffeinewindow.app.db.toDomain
import com.caffeinewindow.app.db.toEntity
import com.caffeinewindow.app.scheduler.DayBoundary
import com.caffeinewindow.app.scheduler.NotificationScheduler
import com.caffeinewindow.app.scheduler.SchedulingMath
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.wake.WakeEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

class WakeRepository(
  private val wakeEventDao: WakeEventDao,
  private val settingsRepository: SettingsRepository,
  private val notificationScheduler: NotificationScheduler,
  private val clock: Clock = Clock.System,
) {
  fun observeLatestWakeEvent(): Flow<WakeEvent?> {
    return wakeEventDao.observeLatest().map { entity -> entity?.toDomain() }
  }

  fun observeTodayWakeEvent(): Flow<WakeEvent?> {
    return combine(
      wakeEventDao.observeAll(),
      settingsRepository.observeSettings(),
    ) { events, settings ->
      val timeZone = TimeZone.currentSystemDefault()
      val logicalDayStart = DayBoundary.logicalDayStart(
        instant = clock.now(),
        resetHour = settings.resetHour,
        timeZone = timeZone,
      )
      events
        .filter { entity ->
          matchesLogicalDay(entity, logicalDayStart, settings.resetHour, timeZone)
        }
        .minByOrNull { it.timestampEpochMillis }
        ?.toDomain()
    }.distinctUntilChanged()
  }

  suspend fun recordWakeEvent(event: WakeEvent): WakeEvent? {
    val settings = settingsRepository.getSettings()
    val timeZone = TimeZone.currentSystemDefault()
    val logicalDayStart = DayBoundary.logicalDayStart(
      instant = event.timestamp,
      resetHour = settings.resetHour,
      timeZone = timeZone,
    )
    val existing = wakeEventDao.getFirstForLogicalDay(logicalDayStart.toEpochMilliseconds())
    if (existing != null) {
      return null
    }

    val insertedId = wakeEventDao.insert(event.toEntity(logicalDayStart))
    if (insertedId == -1L) {
      // Unique index rejected a concurrent insert for the same logical day.
      return null
    }

    val recorded = WakeEventEntity(
      id = insertedId,
      timestampEpochMillis = event.timestamp.toEpochMilliseconds(),
      source = event.source,
      logicalDayStartEpochMillis = logicalDayStart.toEpochMilliseconds(),
    )

    // A late or replayed event can belong to a past logical day. Keep it as history,
    // but only today's wake drives the notification.
    val todayStart = DayBoundary.logicalDayStart(clock.now(), settings.resetHour, timeZone)
    if (logicalDayStart == todayStart) {
      scheduleOrNotify(event, logicalDayStart, settings)
    }

    return recorded.toDomain()
  }

  /**
   * Re-schedules today's caffeine-window notification from today's wake only.
   * Past-day wakes must not trigger an immediate re-show when settings change.
   */
  suspend fun rescheduleFromLatestWake() {
    val settings = settingsRepository.getSettings()
    val timeZone = TimeZone.currentSystemDefault()
    val logicalDayStart = DayBoundary.logicalDayStart(clock.now(), settings.resetHour, timeZone)
    val todayWake = getTodayWakeEvent(logicalDayStart, settings.resetHour, timeZone) ?: run {
      notificationScheduler.cancelScheduledNotification()
      return
    }
    scheduleOrNotify(todayWake, logicalDayStart, settings)
  }

  /**
   * The user changed the delay. If that moves today's window into the future, it is a
   * new window and gets its own notification, even if the old one was already shown.
   * Every other reschedule keeps the one-notification-per-day guard.
   */
  suspend fun onDelayChanged() {
    val settings = settingsRepository.getSettings()
    val timeZone = TimeZone.currentSystemDefault()
    val logicalDayStart = DayBoundary.logicalDayStart(clock.now(), settings.resetHour, timeZone)
    val todayWake = getTodayWakeEvent(logicalDayStart, settings.resetHour, timeZone)
    if (todayWake != null &&
      SchedulingMath.targetNotificationTime(todayWake.timestamp, settings.delayMinutes) > clock.now()
    ) {
      settingsRepository.clearLastNotifiedLogicalDay()
    }
    rescheduleFromLatestWake()
  }

  /** Called once the caffeine-window notification for [logicalDayStart] has been shown. */
  suspend fun markCaffeineWindowNotified(logicalDayStart: Instant) {
    settingsRepository.setLastNotifiedLogicalDay(logicalDayStart.toEpochMilliseconds())
  }

  /**
   * Recomputes stored logical-day keys after [resetHour] changes, keeping the
   * earliest wake per new logical day and dropping the rest, then reschedules.
   */
  suspend fun onResetHourChanged(resetHour: Int) {
    val timeZone = TimeZone.currentSystemDefault()
    val keepers = wakeEventDao.getAll()
      .map { entity ->
        val timestamp = Instant.fromEpochMilliseconds(entity.timestampEpochMillis)
        val newDayStart = DayBoundary.logicalDayStart(timestamp, resetHour, timeZone)
          .toEpochMilliseconds()
        entity to newDayStart
      }
      .groupBy { (_, dayStart) -> dayStart }
      .map { (dayStart, group) ->
        val keeper = group.minBy { (entity, _) -> entity.timestampEpochMillis }.first
        WakeEventEntity(
          id = 0,
          timestampEpochMillis = keeper.timestampEpochMillis,
          source = keeper.source,
          logicalDayStartEpochMillis = dayStart,
        )
      }
      .sortedBy { it.timestampEpochMillis }

    // Rebuild rows so unique-index updates cannot collide mid-migration.
    wakeEventDao.replaceAll(keepers)

    rescheduleFromLatestWake()
  }

  /**
   * Schedules the alarm for a future window, or shows the notification now if the
   * window already opened and today's notification has not been shown yet. At most
   * one caffeine-window notification is shown per logical day.
   */
  private suspend fun scheduleOrNotify(
    wake: WakeEvent,
    logicalDayStart: Instant,
    settings: AppSettings,
  ) {
    if (settingsRepository.getLastNotifiedLogicalDay() == logicalDayStart.toEpochMilliseconds()) {
      notificationScheduler.cancelScheduledNotification()
      return
    }
    val target = SchedulingMath.targetNotificationTime(wake.timestamp, settings.delayMinutes)
    if (target > clock.now()) {
      notificationScheduler.scheduleCaffeineWindowNotification(
        triggerAt = target,
        logicalDayStart = logicalDayStart,
        notificationTitle = settings.notificationTitle,
        notificationBody = settings.notificationBody,
      )
    } else {
      notificationScheduler.cancelScheduledNotification()
      notificationScheduler.showCaffeineWindowNotification(
        notificationTitle = settings.notificationTitle,
        notificationBody = settings.notificationBody,
      )
      markCaffeineWindowNotified(logicalDayStart)
    }
  }

  private suspend fun getTodayWakeEvent(
    logicalDayStart: Instant,
    resetHour: Int,
    timeZone: TimeZone,
  ): WakeEvent? {
    return wakeEventDao.getAll()
      .filter { entity ->
        matchesLogicalDay(entity, logicalDayStart, resetHour, timeZone)
      }
      .minByOrNull { it.timestampEpochMillis }
      ?.toDomain()
  }

  private fun matchesLogicalDay(
    entity: WakeEventEntity,
    logicalDayStart: Instant,
    resetHour: Int,
    timeZone: TimeZone,
  ): Boolean {
    val timestamp = Instant.fromEpochMilliseconds(entity.timestampEpochMillis)
    return DayBoundary.logicalDayStart(timestamp, resetHour, timeZone) == logicalDayStart
  }
}
