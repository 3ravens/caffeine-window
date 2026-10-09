package com.caffeinewindow.app.crash

interface CrashReporter {
  fun log(message: String)
  fun recordException(throwable: Throwable)
}

class NoOpCrashReporter : CrashReporter {
  override fun log(message: String) = Unit

  override fun recordException(throwable: Throwable) = Unit
}
