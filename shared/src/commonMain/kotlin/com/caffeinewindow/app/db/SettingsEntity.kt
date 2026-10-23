package com.caffeinewindow.app.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "settings")
data class SettingsEntity(
  @PrimaryKey val key: String,
  val value: String,
)

@Dao
interface SettingsDao {
  @Query("SELECT * FROM settings")
  fun observeAll(): Flow<List<SettingsEntity>>

  @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
  suspend fun getValue(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(setting: SettingsEntity)

  @Query("DELETE FROM settings WHERE key = :key")
  suspend fun delete(key: String)
}

object SettingsKeys {
  const val DELAY_MINUTES = "delay_minutes"
  const val NOTIFICATION_TITLE = "notification_title"
  const val NOTIFICATION_BODY = "notification_body"
  const val RESET_HOUR = "reset_hour"
  const val LAST_NOTIFIED_LOGICAL_DAY = "last_notified_logical_day"
}
