package com.caffeinewindow.app.wake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant

class FakeWakeSignalSourceTest {
  @Test
  fun emitsWakeEvents() = runTest {
    val source = FakeWakeSignalSource()
    val timestamp = Instant.parse("2025-06-25T06:00:00Z")

    source.emit(timestamp, source = "fake_test")
    val collected = source.observeWakeEvents().first()

    assertEquals(timestamp, collected.timestamp)
    assertEquals("fake_test", collected.source)
  }
}
