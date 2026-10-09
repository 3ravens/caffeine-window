package com.caffeinewindow.app.signal

import android.util.Log
import com.caffeinewindow.app.CaffeineLog
import com.caffeinewindow.app.wake.WakeEvent
import com.caffeinewindow.app.wake.WakeSignalSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock

@Singleton
class ScreenUnlockSignalSource @Inject constructor() : WakeSignalSource {
  // replay = 1 so a collector that subscribes slightly after an unlock (e.g. the
  // service is mid-(re)start) still receives the most recent event instead of
  // dropping it. Per-day dedup in WakeRepository makes a replayed event harmless.
  private val events = MutableSharedFlow<WakeEvent>(
    replay = 1,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override fun observeWakeEvents(): Flow<WakeEvent> = events

  fun onScreenUnlocked() {
    val event = WakeEvent(timestamp = Clock.System.now(), source = SOURCE_NAME)
    val accepted = events.tryEmit(event)
    Log.d(CaffeineLog.TAG, "ScreenUnlockSignalSource.onScreenUnlocked emitted=$accepted at ${event.timestamp}")
  }

  companion object {
    const val SOURCE_NAME = "screen_unlock"
  }
}
