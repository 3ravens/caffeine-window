package com.caffeinewindow.app.repository

import com.caffeinewindow.app.db.WakeEventDao
import com.caffeinewindow.app.db.WakeEventEntity
import com.caffeinewindow.app.db.toDomain
import com.caffeinewindow.app.db.toEntity
import com.caffeinewindow.app.scheduler.DayBoundary
import com.caffeinewindow.app.scheduler.NotificationScheduler
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
        instant = Clock.System.now(),
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

    notificationScheduler.scheduleCaffeineWindowNotification(
      wakeTimestamp = event.timestamp,
      delayMinutes = settings.delayMinutes,
      notificationTitle = settings.notificationTitle,
      notificationBody = settings.notificationBody,
    )

    return recorded.toDomain()
  }

  /**
   * Re-schedules today's caffeine-window notification from today's wake only.
   * Past-day wakes must not trigger an immediate re-show when settings change.
   */
  suspend fun rescheduleFromLatestWake() {
    val settings = settingsRepository.getSettings()
    val todayWake = getTodayWakeEvent(settings.resetHour) ?: run {
      notificationScheduler.cancelScheduledNotification()
      return
    }
    notificationScheduler.scheduleCaffeineWindowNotification(
      wakeTimestamp = todayWake.timestamp,
      delayMinutes = settings.delayMinutes,
      notificationTitle = settings.notificationTitle,
      notificationBody = settings.notificationBody,
    )
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

    // Rebuild rows so unique-index updates cannot collide mid-migration.
    wakeEventDao.deleteAll()
    for (keeper in keepers.sortedBy { it.timestampEpochMillis }) {
      wakeEventDao.insert(keeper)
    }

    rescheduleFromLatestWake()
  }

  private suspend fun getTodayWakeEvent(resetHour: Int): WakeEvent? {
    val timeZone = TimeZone.currentSystemDefault()
    val logicalDayStart = DayBoundary.logicalDayStart(
      instant = Clock.System.now(),
      resetHour = resetHour,
      timeZone = timeZone,
    )
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
