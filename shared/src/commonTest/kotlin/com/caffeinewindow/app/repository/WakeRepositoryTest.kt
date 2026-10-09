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
import kotlin.time.Duration.Companion.hours
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
    val repository = WakeRepository(dao, settingsRepository, scheduler)

    val first = WakeEvent(Instant.parse("2025-06-25T06:00:00Z"), "screen_unlock")
    val second = WakeEvent(Instant.parse("2025-06-25T08:00:00Z"), "screen_unlock")

    val recordedFirst = repository.recordWakeEvent(first)
    val recordedSecond = repository.recordWakeEvent(second)

    assertNotNull(recordedFirst)
    assertNull(recordedSecond)
    assertEquals(1, dao.insertedCount)
    assertEquals(1, scheduler.scheduleCount)
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
      Instant.fromEpochMilliseconds(now.toEpochMilliseconds()),
      scheduler.lastWakeTimestamp,
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
}

private class RecordingNotificationScheduler : NotificationScheduler {
  var scheduleCount = 0
  var cancelCount = 0
  var lastWakeTimestamp: Instant? = null

  override suspend fun scheduleCaffeineWindowNotification(
    wakeTimestamp: Instant,
    delayMinutes: Int,
    notificationTitle: String,
    notificationBody: String,
  ) {
    scheduleCount++
    lastWakeTimestamp = wakeTimestamp
  }

  override suspend fun cancelScheduledNotification() {
    cancelCount++
  }
}
