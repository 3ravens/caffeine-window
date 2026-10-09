package com.caffeinewindow.app.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.repository.SettingsRepository
import com.caffeinewindow.app.repository.WakeRepository
import com.caffeinewindow.app.scheduler.SchedulingMath
import com.caffeinewindow.app.wake.WakeEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

enum class HomeUiState {
  IDLE,
  COUNTDOWN,
  WINDOW_OPEN,
}

data class HomeScreenState(
  val uiState: HomeUiState = HomeUiState.IDLE,
  val wakeEvent: WakeEvent? = null,
  val lastWakeEvent: WakeEvent? = null,
  val remaining: Duration = Duration.ZERO,
  val delayMinutes: Int = 90,
  val permissionStatus: PermissionStatus = PermissionStatus(
    notificationsGranted = true,
    exactAlarmsGranted = true,
    batteryOptimizationDisabled = true,
  ),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val wakeRepository: WakeRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(HomeScreenState())
  val state: StateFlow<HomeScreenState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        wakeRepository.observeTodayWakeEvent(),
        wakeRepository.observeLatestWakeEvent(),
        settingsRepository.observeSettings(),
      ) { todayWakeEvent, lastWakeEvent, settings ->
        Triple(todayWakeEvent, lastWakeEvent, settings.delayMinutes)
      }.collect { (todayWakeEvent, lastWakeEvent, delayMinutes) ->
        updateFromWake(todayWakeEvent, lastWakeEvent, delayMinutes)
      }
    }

    viewModelScope.launch {
      while (isActive) {
        delay(1_000)
        val current = _state.value
        updateFromWake(current.wakeEvent, current.lastWakeEvent, current.delayMinutes)
      }
    }

    refreshPermissionStatus()
  }

  fun refreshPermissionStatus() {
    _state.update {
      it.copy(permissionStatus = PermissionHelper.getStatus(context))
    }
  }

  private fun updateFromWake(
    wakeEvent: WakeEvent?,
    lastWakeEvent: WakeEvent?,
    delayMinutes: Int,
  ) {
    val now = Clock.System.now()
    val permissions = _state.value.permissionStatus
    if (wakeEvent == null) {
      _state.update {
        HomeScreenState(
          uiState = HomeUiState.IDLE,
          lastWakeEvent = lastWakeEvent,
          delayMinutes = delayMinutes,
          permissionStatus = permissions,
        )
      }
      return
    }

    if (SchedulingMath.isWindowOpen(wakeEvent.timestamp, delayMinutes, now)) {
      _state.update {
        HomeScreenState(
          uiState = HomeUiState.WINDOW_OPEN,
          wakeEvent = wakeEvent,
          lastWakeEvent = lastWakeEvent,
          delayMinutes = delayMinutes,
          remaining = Duration.ZERO,
          permissionStatus = permissions,
        )
      }
    } else {
      _state.update {
        HomeScreenState(
          uiState = HomeUiState.COUNTDOWN,
          wakeEvent = wakeEvent,
          lastWakeEvent = lastWakeEvent,
          delayMinutes = delayMinutes,
          remaining = SchedulingMath.remainingDuration(wakeEvent.timestamp, delayMinutes, now),
          permissionStatus = permissions,
        )
      }
    }
  }
}
