package com.caffeinewindow.app.di

import android.app.AlarmManager
import android.content.Context
import com.caffeinewindow.app.db.CaffeineDatabase
import com.caffeinewindow.app.db.createCaffeineDatabase
import com.caffeinewindow.app.notification.AndroidNotificationScheduler
import com.caffeinewindow.app.repository.SettingsRepository
import com.caffeinewindow.app.repository.WakeRepository
import com.caffeinewindow.app.scheduler.NotificationScheduler
import com.caffeinewindow.app.settings.FeatureFlagReader
import com.caffeinewindow.app.settings.FreeTierFeatureFlagReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): CaffeineDatabase {
    return createCaffeineDatabase(context)
  }

  @Provides
  fun provideWakeEventDao(database: CaffeineDatabase) = database.wakeEventDao()

  @Provides
  fun provideSettingsDao(database: CaffeineDatabase) = database.settingsDao()

  @Provides
  @Singleton
  fun provideSettingsRepository(settingsDao: com.caffeinewindow.app.db.SettingsDao): SettingsRepository {
    return SettingsRepository(settingsDao)
  }

  @Provides
  @Singleton
  fun provideNotificationScheduler(
    scheduler: AndroidNotificationScheduler,
  ): NotificationScheduler = scheduler

  @Provides
  @Singleton
  fun provideWakeRepository(
    wakeEventDao: com.caffeinewindow.app.db.WakeEventDao,
    settingsRepository: SettingsRepository,
    notificationScheduler: NotificationScheduler,
  ): WakeRepository {
    return WakeRepository(wakeEventDao, settingsRepository, notificationScheduler)
  }

  @Provides
  @Singleton
  fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
    return context.getSystemService(AlarmManager::class.java)
  }

  @Provides
  @Singleton
  fun provideFeatureFlagReader(): FeatureFlagReader = FreeTierFeatureFlagReader()
}
