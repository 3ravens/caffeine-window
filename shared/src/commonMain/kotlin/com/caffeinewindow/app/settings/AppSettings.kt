package com.caffeinewindow.app.settings

enum class DelayOption(val minutes: Int, val label: String) {
  SIXTY(60, "60 min"),
  NINETY(90, "90 min"),
  ONE_TWENTY(120, "120 min"),
  ;

  companion object {
    fun fromMinutes(minutes: Int): DelayOption {
      return entries.firstOrNull { it.minutes == minutes } ?: NINETY
    }
  }
}

data class AppSettings(
  val delayMinutes: Int = DelayOption.NINETY.minutes,
  val notificationTitle: String = DEFAULT_NOTIFICATION_TITLE,
  val notificationBody: String = DEFAULT_NOTIFICATION_BODY,
  val resetHour: Int = DEFAULT_RESET_HOUR,
) {
  val delayOption: DelayOption
    get() = DelayOption.fromMinutes(delayMinutes)

  companion object {
    const val DEFAULT_RESET_HOUR = 3
    const val DEFAULT_NOTIFICATION_TITLE = "Caffeine window is open"
    const val DEFAULT_NOTIFICATION_BODY =
      "Your delay window has passed — you can try caffeine now if you want."
  }
}
