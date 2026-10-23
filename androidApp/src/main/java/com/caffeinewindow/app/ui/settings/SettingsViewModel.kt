package com.caffeinewindow.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.repository.SettingsRepository
import com.caffeinewindow.app.repository.WakeRepository
import com.caffeinewindow.app.scheduler.SchedulingMath
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.settings.DelayOption
import com.caffeinewindow.app.ui.format.formatClockTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/** One-shot confirmations shown in the Settings snackbar. Copy is resolved in the UI. */
sealed interface SettingsMessage {
    data class DelaySaved(
        val minutes: Int,
        val opensAtClock: String?,
        val alreadyOpen: Boolean,
    ) : SettingsMessage

    data object CopySaved : SettingsMessage

    data class DayStartSaved(val hour: Int) : SettingsMessage
}

data class SettingsScreenState(
    val settings: AppSettings = AppSettings(),
    val permissionStatus: PermissionStatus = PermissionStatus(
        notificationsGranted = true,
        exactAlarmsGranted = true,
        batteryOptimizationDisabled = true,
    ),
    val monitorChannelHidden: Boolean = false,
    val message: SettingsMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val wakeRepository: WakeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state: StateFlow<SettingsScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        refreshPermissionStatus()
    }

    fun refreshPermissionStatus() {
        _state.update {
            it.copy(
                permissionStatus = PermissionHelper.getStatus(context),
                monitorChannelHidden = PermissionHelper.isMonitorChannelMuted(context),
            )
        }
    }

    fun setDelay(option: DelayOption) {
        viewModelScope.launch {
            settingsRepository.updateDelayMinutes(option.minutes)
            wakeRepository.onDelayChanged()
            // Reading back tells us whether today's window is already open, so the snackbar
            // does not promise an open time that has already passed.
            val wake = wakeRepository.observeTodayWakeEvent().first()
            val message = if (wake == null) {
                SettingsMessage.DelaySaved(option.minutes, opensAtClock = null, alreadyOpen = false)
            } else {
                val target = SchedulingMath.targetNotificationTime(wake.timestamp, option.minutes)
                SettingsMessage.DelaySaved(
                    minutes = option.minutes,
                    opensAtClock = formatClockTime(target),
                    alreadyOpen = target <= Clock.System.now(),
                )
            }
            _state.update { it.copy(message = message) }
        }
    }

    fun updateNotificationCopy(title: String, body: String) {
        if (title.isBlank() || body.isBlank()) return
        viewModelScope.launch {
            settingsRepository.updateNotificationCopy(title, body)
            wakeRepository.rescheduleFromLatestWake()
            _state.update { it.copy(message = SettingsMessage.CopySaved) }
        }
    }

    fun setResetHour(hour: Int) {
        if (hour !in 0..23) return
        viewModelScope.launch {
            settingsRepository.updateResetHour(hour)
            wakeRepository.onResetHourChanged(hour)
            _state.update { it.copy(message = SettingsMessage.DayStartSaved(hour)) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
