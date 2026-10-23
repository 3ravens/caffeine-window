package com.caffeinewindow.app.repository

import com.caffeinewindow.app.db.SettingsDao
import com.caffeinewindow.app.db.SettingsEntity
import com.caffeinewindow.app.db.SettingsKeys
import com.caffeinewindow.app.db.WakeEventDao
import com.caffeinewindow.app.db.WakeEventEntity
import com.caffeinewindow.app.scheduler.DayBoundary
import com.caffeinewindow.app.scheduler.NotificationScheduler
import com.caffeinewindow.app.wake.WakeEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class WakeRepositoryTest {
  @Test
  fun recordsOnlyFirstWakePerLogicalDay() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val first = WakeEvent(Instant.parse("2025-06-25T06:00:00Z"), "screen_unlock")
    val second = WakeEvent(Instant.parse("2025-06-25T06:10:00Z"), "screen_unlock")
    val repository = WakeRepository(
      dao,
      settingsRepository,
      scheduler,
      FixedClock(first.timestamp + 15.minutes),
    )

    val recordedFirst = repository.recordWakeEvent(first)
    val recordedSecond = repository.recordWakeEvent(second)

    assertNotNull(recordedFirst)
    assertNull(recordedSecond)
    assertEquals(1, dao.insertedCount)
    assertEquals(1, scheduler.scheduleCount)
    assertEquals(first.timestamp + 90.minutes, scheduler.lastTriggerAt)
  }

  @Test
  fun pastDayWakeIsStoredButNotScheduled() = runTest {
    val dao = FakeWakeEventDao()
    val scheduler = RecordingNotificationScheduler()
    val stale = WakeEvent(Instant.parse("2025-06-24T06:00:00Z"), "screen_unlock")
    val repository = WakeRepository(
      dao,
      SettingsRepository(FakeSettingsDao()),
      scheduler,
      FixedClock(stale.timestamp + 2.days),
    )

    assertNotNull(repository.recordWakeEvent(stale))

    assertEquals(0, scheduler.scheduleCount)
    assertEquals(0, scheduler.showCount)
  }

  @Test
  fun rescheduleAfterWindowOpenShowsOnlyOncePerDay() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val wake = morningWake()
    val repository = WakeRepository(
      dao,
      settingsRepository,
      scheduler,
      FixedClock(wake + 2.hours),
    )
    seedTodayWake(dao, wake)

    // e.g. the alarm was lost to a reboot that spanned the window opening
    repository.rescheduleFromLatestWake()
    // e.g. a settings change or service restart later the same day
    repository.rescheduleFromLatestWake()

    assertEquals(1, scheduler.showCount)
    assertEquals(0, scheduler.scheduleCount)
  }

  @Test
  fun rescheduleSkipsDayAlreadyNotifiedByAlarm() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val wake = morningWake()
    val repository = WakeRepository(
      dao,
      settingsRepository,
      scheduler,
      FixedClock(wake + 30.minutes),
    )
    val dayStart = seedTodayWake(dao, wake)
    repository.markCaffeineWindowNotified(dayStart)

    repository.rescheduleFromLatestWake()

    assertEquals(0, scheduler.showCount)
    assertEquals(0, scheduler.scheduleCount)
    assertEquals(1, scheduler.cancelCount)
  }

  @Test
  fun delayChangeMovingWindowLaterReArmsNotification() = runTest {
    // Woke 08:00, 60-min window opened and notified at 09:00; at 09:15 the user picks 90 min.
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(
      FakeSettingsDao(initial = mapOf(SettingsKeys.DELAY_MINUTES to "60")),
    )
    val scheduler = RecordingNotificationScheduler()
    val wake = morningWake()
    val repository = WakeRepository(dao, settingsRepository, scheduler, FixedClock(wake + 75.minutes))
    val dayStart = seedTodayWake(dao, wake)
    repository.markCaffeineWindowNotified(dayStart)

    settingsRepository.updateDelayMinutes(90)
    repository.onDelayChanged()

    assertEquals(1, scheduler.scheduleCount)
    assertEquals(wake + 90.minutes, scheduler.lastTriggerAt)
    assertEquals(0, scheduler.showCount)
  }

  @Test
  fun delayChangeKeepingWindowOpenDoesNotNotifyAgain() = runTest {
    // Window already notified; the new delay still puts the opening in the past.
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(
      FakeSettingsDao(initial = mapOf(SettingsKeys.DELAY_MINUTES to "60")),
    )
    val scheduler = RecordingNotificationScheduler()
    val wake = morningWake()
    val repository = WakeRepository(dao, settingsRepository, scheduler, FixedClock(wake + 3.hours))
    val dayStart = seedTodayWake(dao, wake)
    repository.markCaffeineWindowNotified(dayStart)

    settingsRepository.updateDelayMinutes(120)
    repository.onDelayChanged()

    assertEquals(0, scheduler.scheduleCount)
    assertEquals(0, scheduler.showCount)
  }

  @Test
  fun concurrentDuplicateInsertReturnsNull() = runTest {
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val racingDao = FakeWakeEventDao(hideExistingOnGet = true, rejectNextInsert = true)
    racingDao.seed(
      WakeEventEntity(
        id = 1,
        timestampEpochMillis = Instant.parse("2025-06-25T06:00:00Z").toEpochMilliseconds(),
        source = "screen_unlock",
        logicalDayStartEpochMillis = Instant.parse("2025-06-25T03:00:00Z").toEpochMilliseconds(),
      ),
    )
    val racingRepo = WakeRepository(racingDao, settingsRepository, scheduler)
    val result = racingRepo.recordWakeEvent(
      WakeEvent(Instant.parse("2025-06-25T07:00:00Z"), "screen_unlock"),
    )

    assertNull(result)
    assertEquals(0, scheduler.scheduleCount)
  }

  @Test
  fun rescheduleCancelsWhenOnlyPastDayWakeExists() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(
      FakeSettingsDao(initial = mapOf(SettingsKeys.DELAY_MINUTES to "90")),
    )
    val scheduler = RecordingNotificationScheduler()
    val repository = WakeRepository(dao, settingsRepository, scheduler)

    dao.seed(
      WakeEventEntity(
        id = 1,
        timestampEpochMillis = Instant.parse("2025-06-24T06:00:00Z").toEpochMilliseconds(),
        source = "screen_unlock",
        logicalDayStartEpochMillis = Instant.parse("2025-06-24T03:00:00Z").toEpochMilliseconds(),
      ),
    )

    repository.rescheduleFromLatestWake()

    assertEquals(0, scheduler.scheduleCount)
    assertEquals(1, scheduler.cancelCount)
  }

  @Test
  fun rescheduleSchedulesWhenTodayWakeExists() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val repository = WakeRepository(dao, settingsRepository, scheduler)

    val now = Clock.System.now()
    val dayStart = DayBoundary.logicalDayStart(now, resetHour = 3, TimeZone.currentSystemDefault())
    dao.seed(
      WakeEventEntity(
        id = 1,
        timestampEpochMillis = now.toEpochMilliseconds(),
        source = "screen_unlock",
        logicalDayStartEpochMillis = dayStart.toEpochMilliseconds(),
      ),
    )

    repository.rescheduleFromLatestWake()

    assertEquals(1, scheduler.scheduleCount)
    assertEquals(
      Instant.fromEpochMilliseconds(now.toEpochMilliseconds()) + 90.minutes,
      scheduler.lastTriggerAt,
    )
  }

  @Test
  fun resetHourChangeKeepsEarliestPerNewDay() = runTest {
    val dao = FakeWakeEventDao()
    val settingsRepository = SettingsRepository(FakeSettingsDao())
    val scheduler = RecordingNotificationScheduler()
    val repository = WakeRepository(dao, settingsRepository, scheduler)

    val timeZone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val localHour = now.toLocalDateTime(timeZone).hour
    // Pick a reset hour far from "now" so two recent timestamps share a logical day.
    val safeResetHour = (localHour + 12) % 24
    val earlier = now - 2.hours
    val later = now - 1.hours

    dao.seed(
      WakeEventEntity(
        id = 1,
        timestampEpochMillis = earlier.toEpochMilliseconds(),
        source = "screen_unlock",
        logicalDayStartEpochMillis = DayBoundary.logicalDayStart(earlier, 3, timeZone)
          .toEpochMilliseconds(),
      ),
    )
    dao.seed(
      WakeEventEntity(
        id = 2,
        timestampEpochMillis = later.toEpochMilliseconds(),
        source = "screen_unlock",
        logicalDayStartEpochMillis = DayBoundary.logicalDayStart(later, 3, timeZone)
          .toEpochMilliseconds(),
      ),
    )

    repository.onResetHourChanged(resetHour = safeResetHour)

    val remaining = dao.getAll()
    assertEquals(1, remaining.size)
    assertEquals(earlier.toEpochMilliseconds(), remaining.single().timestampEpochMillis)
    assertTrue(scheduler.scheduleCount + scheduler.cancelCount >= 1)
  }
}

private class FakeWakeEventDao(
  private val rejectNextInsert: Boolean = false,
  private val hideExistingOnGet: Boolean = false,
) : WakeEventDao {
  private val events = mutableListOf<WakeEventEntity>()
  var insertedCount = 0

  fun seed(event: WakeEventEntity) {
    events.add(event)
  }

  override suspend fun insert(event: WakeEventEntity): Long {
    insertedCount++
    if (rejectNextInsert) return -1L
    val existing = events.firstOrNull {
      it.logicalDayStartEpochMillis == event.logicalDayStartEpochMillis
    }
    if (existing != null) return -1L
    val id = (events.maxOfOrNull { it.id } ?: 0L) + 1L
    events.add(event.copy(id = id))
    return id
  }

  override suspend fun getFirstForLogicalDay(logicalDayStart: Long): WakeEventEntity? {
    if (hideExistingOnGet) return null
    return events.firstOrNull { it.logicalDayStartEpochMillis == logicalDayStart }
  }

  override suspend fun getLatest(): WakeEventEntity? = events.maxByOrNull { it.timestampEpochMillis }

  override suspend fun getAll(): List<WakeEventEntity> = events.toList()

  override suspend fun updateLogicalDayStart(id: Long, logicalDayStart: Long) {
    val index = events.indexOfFirst { it.id == id }
    if (index >= 0) {
      events[index] = events[index].copy(logicalDayStartEpochMillis = logicalDayStart)
    }
  }

  override suspend fun deleteById(id: Long) {
    events.removeAll { it.id == id }
  }

  override suspend fun deleteAll() {
    events.clear()
  }

  override fun observeAll(): Flow<List<WakeEventEntity>> = flowOf(events.toList())

  override fun observeLatest(): Flow<WakeEventEntity?> =
    flowOf(events.maxByOrNull { it.timestampEpochMillis })
}

private class FakeSettingsDao(
  initial: Map<String, String> = emptyMap(),
) : SettingsDao {
  private val values = MutableStateFlow(initial.map { (k, v) -> SettingsEntity(k, v) })

  override fun observeAll(): Flow<List<SettingsEntity>> = values

  override suspend fun getValue(key: String): String? = values.value.find { it.key == key }?.value

  override suspend fun upsert(setting: SettingsEntity) {
    val without = values.value.filterNot { it.key == setting.key }
    values.value = without + setting
  }

  override suspend fun delete(key: String) {
    values.value = values.value.filterNot { it.key == key }
  }
}

/** 08:00 local on a fixed date, so wake + a few hours stays inside one logical day. */
private fun morningWake(): Instant {
  return DayBoundary.logicalDayStart(Instant.parse("2025-06-25T12:00:00Z"), resetHour = 3) + 5.hours
}

private fun seedTodayWake(dao: FakeWakeEventDao, wake: Instant): Instant {
  val dayStart = DayBoundary.logicalDayStart(wake, resetHour = 3)
  dao.seed(
    WakeEventEntity(
      id = 1,
      timestampEpochMillis = wake.toEpochMilliseconds(),
      source = "screen_unlock",
      logicalDayStartEpochMillis = dayStart.toEpochMilliseconds(),
    ),
  )
  return dayStart
}

private class FixedClock(private val instant: Instant) : Clock {
  override fun now(): Instant = instant
}

private class RecordingNotificationScheduler : NotificationScheduler {
  var scheduleCount = 0
  var showCount = 0
  var cancelCount = 0
  var lastTriggerAt: Instant? = null

  override suspend fun scheduleCaffeineWindowNotification(
    triggerAt: Instant,
    logicalDayStart: Instant,
    notificationTitle: String,
    notificationBody: String,
  ) {
    scheduleCount++
    lastTriggerAt = triggerAt
  }

  override suspend fun showCaffeineWindowNotification(
    notificationTitle: String,
    notificationBody: String,
  ) {
    showCount++
  }

  override suspend fun cancelScheduledNotification() {
    cancelCount++
  }
}
