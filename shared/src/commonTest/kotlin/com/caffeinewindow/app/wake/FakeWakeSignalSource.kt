package com.caffeinewindow.app.wake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Instant

/**
 * Test-only wake signal source. Deliberately unlike any wearable integration.
 */
class FakeWakeSignalSource : WakeSignalSource {
  private val events = MutableSharedFlow<WakeEvent>(
    replay = 1,
    extraBufferCapacity = 1,
  )

  override fun observeWakeEvents(): Flow<WakeEvent> = events

  suspend fun emit(timestamp: Instant, source: String = "fake") {
    events.emit(WakeEvent(timestamp = timestamp, source = source))
  }
}
