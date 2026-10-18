package com.caffeinewindow.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.caffeinewindow.app.scheduler.NotificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

@Singleton
class AndroidNotificationScheduler @Inject constructor(
  @ApplicationContext private val context: Context,
  private val alarmManager: AlarmManager,
) : NotificationScheduler {
  override suspend fun scheduleCaffeineWindowNotification(
    triggerAt: Instant,
    logicalDayStart: Instant,
    notificationTitle: String,
    notificationBody: String,
  ) {
    // Cancel any existing alarm first. Building the real PendingIntent must come
    // last, because FLAG_UPDATE_CURRENT mutates the shared PendingIntent's extras
    // (Android matches PendingIntents ignoring extras), so a later empty build
    // would otherwise clobber the extras the AlarmReceiver needs.
    cancelScheduledNotification()

    val triggerAtMillis = triggerAt.toEpochMilliseconds()
    val pendingIntent = buildAlarmPendingIntent(
      title = notificationTitle,
      body = notificationBody,
      logicalDayStartMillis = logicalDayStart.toEpochMilliseconds(),
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    } else {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }

  override suspend fun showCaffeineWindowNotification(
    notificationTitle: String,
    notificationBody: String,
  ) {
    NotificationFactory(context).showCaffeineWindowNotification(notificationTitle, notificationBody)
  }

  override suspend fun cancelScheduledNotification() {
    alarmManager.cancel(buildAlarmPendingIntent(title = "", body = "", logicalDayStartMillis = 0L))
  }

  private fun buildAlarmPendingIntent(
    title: String,
    body: String,
    logicalDayStartMillis: Long,
  ): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_SHOW_CAFFEINE_NOTIFICATION
      putExtra(AlarmReceiver.EXTRA_TITLE, title)
      putExtra(AlarmReceiver.EXTRA_BODY, body)
      putExtra(AlarmReceiver.EXTRA_LOGICAL_DAY_START, logicalDayStartMillis)
    }
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  companion object {
    private const val ALARM_REQUEST_CODE = 42_001
  }
}
