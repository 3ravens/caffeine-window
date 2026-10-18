package com.caffeinewindow.app.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Every schema bump needs an entry here (or an `AutoMigration`). There is no destructive
 * fallback: a missing migration must fail loudly in testing, not wipe wake history.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(connection: SQLiteConnection) {
    // v2 adds a unique index per logical day. Keep the first-inserted row per day so
    // the index can be created on databases that collected duplicates under v1.
    connection.execSQL(
      "DELETE FROM wake_events WHERE id NOT IN " +
        "(SELECT MIN(id) FROM wake_events GROUP BY logicalDayStartEpochMillis)",
    )
    connection.execSQL(
      "CREATE UNIQUE INDEX IF NOT EXISTS `index_wake_events_logicalDayStartEpochMillis` " +
        "ON `wake_events` (`logicalDayStartEpochMillis`)",
    )
  }
}

internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
