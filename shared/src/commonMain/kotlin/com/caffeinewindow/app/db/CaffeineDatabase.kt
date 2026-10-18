package com.caffeinewindow.app.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
  entities = [
    WakeEventEntity::class,
    SettingsEntity::class,
    FeatureFlagEntity::class,
  ],
  version = 2,
)
@ConstructedBy(CaffeineDatabaseConstructor::class)
abstract class CaffeineDatabase : RoomDatabase() {
  abstract fun wakeEventDao(): WakeEventDao
  abstract fun settingsDao(): SettingsDao
  abstract fun featureFlagDao(): FeatureFlagDao
}

// Room KSP generates the matching `actual` implementation.
@Suppress("KotlinNoActualForExpect")
expect object CaffeineDatabaseConstructor : RoomDatabaseConstructor<CaffeineDatabase> {
  override fun initialize(): CaffeineDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<CaffeineDatabase>): CaffeineDatabase {
  return builder
    .addMigrations(*ALL_MIGRATIONS)
    // Play never installs a lower versionCode over a higher one; this only covers
    // sideloaded dev builds, where wiping is acceptable.
    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
}
