package com.caffeinewindow.app.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Entity(
  tableName = "wake_events",
  indices = [
    Index(value = ["logicalDayStartEpochMillis"], unique = true),
  ],
)
data class WakeEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val timestampEpochMillis: Long,
  val source: String,
  val logicalDayStartEpochMillis: Long,
)

@Dao
interface WakeEventDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(event: WakeEventEntity): Long

  @Query("SELECT * FROM wake_events WHERE logicalDayStartEpochMillis = :logicalDayStart ORDER BY timestampEpochMillis ASC LIMIT 1")
  suspend fun getFirstForLogicalDay(logicalDayStart: Long): WakeEventEntity?

  @Query("SELECT * FROM wake_events ORDER BY timestampEpochMillis DESC LIMIT 1")
  suspend fun getLatest(): WakeEventEntity?

  @Query("SELECT * FROM wake_events ORDER BY timestampEpochMillis ASC")
  suspend fun getAll(): List<WakeEventEntity>

  @Query("UPDATE wake_events SET logicalDayStartEpochMillis = :logicalDayStart WHERE id = :id")
  suspend fun updateLogicalDayStart(id: Long, logicalDayStart: Long)

  @Query("DELETE FROM wake_events WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM wake_events")
  suspend fun deleteAll()

  @Query("SELECT * FROM wake_events ORDER BY timestampEpochMillis DESC")
  fun observeAll(): Flow<List<WakeEventEntity>>

  @Query("SELECT * FROM wake_events ORDER BY timestampEpochMillis DESC LIMIT 1")
  fun observeLatest(): Flow<WakeEventEntity?>
}

fun WakeEventEntity.toDomain(): com.caffeinewindow.app.wake.WakeEvent {
  return com.caffeinewindow.app.wake.WakeEvent(
    timestamp = Instant.fromEpochMilliseconds(timestampEpochMillis),
    source = source,
  )
}

fun com.caffeinewindow.app.wake.WakeEvent.toEntity(logicalDayStart: Instant): WakeEventEntity {
  return WakeEventEntity(
    timestampEpochMillis = timestamp.toEpochMilliseconds(),
    source = source,
    logicalDayStartEpochMillis = logicalDayStart.toEpochMilliseconds(),
  )
}
