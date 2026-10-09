package com.caffeinewindow.app.repository

import com.caffeinewindow.app.db.SettingsDao
import com.caffeinewindow.app.db.SettingsEntity
import com.caffeinewindow.app.db.SettingsKeys
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.settings.DelayOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SettingsRepositoryTest {
  @Test
  fun getSettingsReturnsDefaultsWhenEmpty() = runTest {
    val repository = SettingsRepository(InMemorySettingsDao())
    val settings = repository.getSettings()
    assertEquals(DelayOption.NINETY.minutes, settings.delayMinutes)
    assertEquals(AppSettings.DEFAULT_RESET_HOUR, settings.resetHour)
    assertEquals(AppSettings.DEFAULT_NOTIFICATION_TITLE, settings.notificationTitle)
  }

  @Test
  fun updateDelayMinutesPersistsValidOption() = runTest {
    val dao = InMemorySettingsDao()
    val repository = SettingsRepository(dao)
    repository.updateDelayMinutes(DelayOption.SIXTY.minutes)
    assertEquals("60", dao.getValue(SettingsKeys.DELAY_MINUTES))
    assertEquals(60, repository.observeSettings().first().delayMinutes)
  }

  @Test
  fun updateDelayMinutesRejectsInvalidValue() = runTest {
    val repository = SettingsRepository(InMemorySettingsDao())
    assertFailsWith<IllegalArgumentException> {
      repository.updateDelayMinutes(45)
    }
  }

  @Test
  fun updateNotificationCopyRejectsBlank() = runTest {
    val repository = SettingsRepository(InMemorySettingsDao())
    assertFailsWith<IllegalArgumentException> {
      repository.updateNotificationCopy(" ", "body")
    }
    assertFailsWith<IllegalArgumentException> {
      repository.updateNotificationCopy("title", "")
    }
  }

  @Test
  fun updateResetHourRejectsOutOfRange() = runTest {
    val repository = SettingsRepository(InMemorySettingsDao())
    assertFailsWith<IllegalArgumentException> {
      repository.updateResetHour(24)
    }
  }

  @Test
  fun ensureDefaultsWritesMissingKeys() = runTest {
    val dao = InMemorySettingsDao()
    val repository = SettingsRepository(dao)
    repository.ensureDefaults()
    assertEquals(DelayOption.NINETY.minutes.toString(), dao.getValue(SettingsKeys.DELAY_MINUTES))
    assertEquals(AppSettings.DEFAULT_NOTIFICATION_TITLE, dao.getValue(SettingsKeys.NOTIFICATION_TITLE))
    assertEquals(AppSettings.DEFAULT_NOTIFICATION_BODY, dao.getValue(SettingsKeys.NOTIFICATION_BODY))
    assertEquals(AppSettings.DEFAULT_RESET_HOUR.toString(), dao.getValue(SettingsKeys.RESET_HOUR))
  }
}

private class InMemorySettingsDao : SettingsDao {
  private val values = MutableStateFlow<List<SettingsEntity>>(emptyList())

  override fun observeAll(): Flow<List<SettingsEntity>> = values

  override suspend fun getValue(key: String): String? = values.value.find { it.key == key }?.value

  override suspend fun upsert(setting: SettingsEntity) {
    val without = values.value.filterNot { it.key == setting.key }
    values.value = without + setting
  }
}
