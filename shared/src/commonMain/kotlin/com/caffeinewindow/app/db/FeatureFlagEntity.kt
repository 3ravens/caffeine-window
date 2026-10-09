package com.caffeinewindow.app.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "feature_flags")
data class FeatureFlagEntity(
  @PrimaryKey val key: String,
  val enabled: Boolean,
)

@Dao
interface FeatureFlagDao {
  @Query("SELECT * FROM feature_flags")
  fun observeAll(): Flow<List<FeatureFlagEntity>>

  @Query("SELECT enabled FROM feature_flags WHERE key = :key LIMIT 1")
  suspend fun isEnabled(key: String): Boolean?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(flag: FeatureFlagEntity)
}
