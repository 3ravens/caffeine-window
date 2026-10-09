package com.caffeinewindow.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.caffeinewindow.app.scheduler.NotificationScheduler
import com.caffeinewindow.app.scheduler.SchedulingMath
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Singleton
class AndroidNotificationScheduler @Inject constructor(
  @ApplicationContext private val context: Context,
  private val alarmManager: AlarmManager,
) : NotificationScheduler {
  override suspend fun scheduleCaffeineWindowNotification(
    wakeTimestamp: Instant,
    delayMinutes: Int,
    notificationTitle: String,
    notificationBody: String,
  ) {
    val now = Clock.System.now()
    val target = SchedulingMath.targetNotificationTime(wakeTimestamp, delayMinutes)

    if (SchedulingMath.isWindowOpen(wakeTimestamp, delayMinutes, now)) {
      cancelScheduledNotification()
      NotificationFactory(context).showCaffeineWindowNotification(notificationTitle, notificationBody)
      return
    }

    // Cancel any existing alarm first. Building the real PendingIntent must come
    // last, because FLAG_UPDATE_CURRENT mutates the shared PendingIntent's extras
    // (Android matches PendingIntents ignoring extras), so a later empty build
    // would otherwise clobber the title/body the AlarmReceiver needs.
    cancelScheduledNotification()

    val triggerAtMillis = target.toEpochMilliseconds()
    val pendingIntent = buildAlarmPendingIntent(notificationTitle, notificationBody)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent,
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent,
      )
    } else {
      alarmManager.setExact(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent,
      )
    }
  }

  override suspend fun cancelScheduledNotification() {
    alarmManager.cancel(buildAlarmPendingIntent("", ""))
  }

  private fun buildAlarmPendingIntent(title: String, body: String): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_SHOW_CAFFEINE_NOTIFICATION
      putExtra(AlarmReceiver.EXTRA_TITLE, title)
      putExtra(AlarmReceiver.EXTRA_BODY, body)
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
