package com.caffeinewindow.app.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<CaffeineDatabase> {
  val appContext = context.applicationContext
  val dbFile = appContext.getDatabasePath("caffeine_window.db")
  return Room.databaseBuilder(
    context = appContext,
    name = dbFile.absolutePath,
  )
}

fun createCaffeineDatabase(context: Context): CaffeineDatabase {
  return getRoomDatabase(getDatabaseBuilder(context))
}
