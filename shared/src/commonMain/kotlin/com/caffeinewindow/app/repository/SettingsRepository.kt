package com.caffeinewindow.app.repository

import com.caffeinewindow.app.db.SettingsDao
import com.caffeinewindow.app.db.SettingsEntity
import com.caffeinewindow.app.db.SettingsKeys
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.settings.DelayOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
  private val settingsDao: SettingsDao,
) {
  fun observeSettings(): Flow<AppSettings> {
    return settingsDao.observeAll().map { entities -> entities.toAppSettings() }
  }

  suspend fun getSettings(): AppSettings {
    val delayMinutes = settingsDao.getValue(SettingsKeys.DELAY_MINUTES)?.toIntOrNull()
      ?: DelayOption.NINETY.minutes
    val notificationTitle = settingsDao.getValue(SettingsKeys.NOTIFICATION_TITLE)
      ?: AppSettings.DEFAULT_NOTIFICATION_TITLE
    val notificationBody = settingsDao.getValue(SettingsKeys.NOTIFICATION_BODY)
      ?: AppSettings.DEFAULT_NOTIFICATION_BODY
    val resetHour = settingsDao.getValue(SettingsKeys.RESET_HOUR)?.toIntOrNull()
      ?: AppSettings.DEFAULT_RESET_HOUR

    return AppSettings(
      delayMinutes = delayMinutes,
      notificationTitle = notificationTitle,
      notificationBody = notificationBody,
      resetHour = resetHour,
    )
  }

  suspend fun updateDelayMinutes(minutes: Int) {
    require(DelayOption.entries.any { it.minutes == minutes }) {
      "delayMinutes must be one of ${DelayOption.entries.map { it.minutes }}"
    }
    settingsDao.upsert(SettingsEntity(SettingsKeys.DELAY_MINUTES, minutes.toString()))
  }

  suspend fun updateNotificationCopy(title: String, body: String) {
    require(title.isNotBlank()) { "notification title must not be blank" }
    require(body.isNotBlank()) { "notification body must not be blank" }
    settingsDao.upsert(SettingsEntity(SettingsKeys.NOTIFICATION_TITLE, title.trim()))
    settingsDao.upsert(SettingsEntity(SettingsKeys.NOTIFICATION_BODY, body.trim()))
  }

  suspend fun updateResetHour(hour: Int) {
    require(hour in 0..23) { "resetHour must be 0-23" }
    settingsDao.upsert(SettingsEntity(SettingsKeys.RESET_HOUR, hour.toString()))
  }

  /** Logical-day start (epoch millis) of the last caffeine-window notification shown. */
  suspend fun getLastNotifiedLogicalDay(): Long? {
    return settingsDao.getValue(SettingsKeys.LAST_NOTIFIED_LOGICAL_DAY)?.toLongOrNull()
  }

  suspend fun setLastNotifiedLogicalDay(logicalDayStartEpochMillis: Long) {
    settingsDao.upsert(
      SettingsEntity(SettingsKeys.LAST_NOTIFIED_LOGICAL_DAY, logicalDayStartEpochMillis.toString()),
    )
  }

  suspend fun clearLastNotifiedLogicalDay() {
    settingsDao.delete(SettingsKeys.LAST_NOTIFIED_LOGICAL_DAY)
  }

  suspend fun ensureDefaults() {
    val current = getSettings()
    if (settingsDao.getValue(SettingsKeys.DELAY_MINUTES) == null) {
      settingsDao.upsert(SettingsEntity(SettingsKeys.DELAY_MINUTES, current.delayMinutes.toString()))
    }
    if (settingsDao.getValue(SettingsKeys.NOTIFICATION_TITLE) == null) {
      settingsDao.upsert(SettingsEntity(SettingsKeys.NOTIFICATION_TITLE, current.notificationTitle))
    }
    if (settingsDao.getValue(SettingsKeys.NOTIFICATION_BODY) == null) {
      settingsDao.upsert(SettingsEntity(SettingsKeys.NOTIFICATION_BODY, current.notificationBody))
    }
    if (settingsDao.getValue(SettingsKeys.RESET_HOUR) == null) {
      settingsDao.upsert(SettingsEntity(SettingsKeys.RESET_HOUR, current.resetHour.toString()))
    }
  }

  private fun List<SettingsEntity>.toAppSettings(): AppSettings {
    val map = associate { it.key to it.value }
    return AppSettings(
      delayMinutes = map[SettingsKeys.DELAY_MINUTES]?.toIntOrNull() ?: DelayOption.NINETY.minutes,
      notificationTitle = map[SettingsKeys.NOTIFICATION_TITLE] ?: AppSettings.DEFAULT_NOTIFICATION_TITLE,
      notificationBody = map[SettingsKeys.NOTIFICATION_BODY] ?: AppSettings.DEFAULT_NOTIFICATION_BODY,
      resetHour = map[SettingsKeys.RESET_HOUR]?.toIntOrNull() ?: AppSettings.DEFAULT_RESET_HOUR,
    )
  }
}
