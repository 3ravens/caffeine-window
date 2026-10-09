package com.caffeinewindow.app.wake

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

data class WakeEvent(
  val timestamp: Instant,
  val source: String,
)

interface WakeSignalSource {
  fun observeWakeEvents(): Flow<WakeEvent>
}
