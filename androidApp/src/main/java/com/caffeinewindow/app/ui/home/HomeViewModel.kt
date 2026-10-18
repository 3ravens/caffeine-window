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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class HomeUiState {
    /** Before the first database emission. Prevents the idle copy flashing on cold start. */
    LOADING,
    IDLE,
    COUNTDOWN,
    WINDOW_OPEN,
}

data class HomeScreenState(
    val uiState: HomeUiState = HomeUiState.LOADING,
    val wakeEvent: WakeEvent? = null,
    val lastWakeEvent: WakeEvent? = null,
    val delayMinutes: Int = 90,
    /** Whole minutes until the window opens, rounded up, so it reads "1 min" until then. */
    val remainingMinutes: Int = 0,
    /** 0f..1f progress from wake event to window open, for the ring. */
    val elapsedFraction: Float = 0f,
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
            // Tick every second so the window-open flip is prompt, but only publish a new
            // state when the derived minute value or the state itself actually changes.
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
        val next = if (wakeEvent == null) {
            HomeScreenState(
                uiState = HomeUiState.IDLE,
                lastWakeEvent = lastWakeEvent,
                delayMinutes = delayMinutes,
                permissionStatus = permissions,
            )
        } else {
            countdownState(wakeEvent, lastWakeEvent, delayMinutes, now, permissions)
        }
        if (next != _state.value) {
            _state.value = next
        }
    }

    private fun countdownState(
        wakeEvent: WakeEvent,
        lastWakeEvent: WakeEvent?,
        delayMinutes: Int,
        now: Instant,
        permissions: PermissionStatus,
    ): HomeScreenState {
        val target = SchedulingMath.targetNotificationTime(wakeEvent.timestamp, delayMinutes)
        val remaining = target - now
        val windowOpen = remaining.inWholeMilliseconds <= 0L
        val remainingMinutes = if (windowOpen) {
            0
        } else {
            ((remaining.inWholeSeconds + 59) / 60).toInt().coerceAtLeast(1)
        }
        val elapsedFraction = if (windowOpen) {
            1f
        } else {
            val totalMillis = (delayMinutes * 60_000L).toFloat()
            ((now - wakeEvent.timestamp).inWholeMilliseconds / totalMillis).coerceIn(0f, 1f)
        }
        return HomeScreenState(
            uiState = if (windowOpen) HomeUiState.WINDOW_OPEN else HomeUiState.COUNTDOWN,
            wakeEvent = wakeEvent,
            lastWakeEvent = lastWakeEvent,
            delayMinutes = delayMinutes,
            remainingMinutes = remainingMinutes,
            elapsedFraction = elapsedFraction,
            permissionStatus = permissions,
        )
    }
}
