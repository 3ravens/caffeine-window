package com.caffeinewindow.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.repository.SettingsRepository
import com.caffeinewindow.app.repository.WakeRepository
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.settings.DelayOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsScreenState(
  val settings: AppSettings = AppSettings(),
  val permissionStatus: PermissionStatus = PermissionStatus(
    notificationsGranted = true,
    exactAlarmsGranted = true,
    batteryOptimizationDisabled = true,
  ),
  val monitorChannelHidden: Boolean = false,
  val message: String? = null,
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
      wakeRepository.rescheduleFromLatestWake()
      _state.update { it.copy(message = "Delay updated to ${option.label}") }
    }
  }

  fun updateNotificationCopy(title: String, body: String) {
    viewModelScope.launch {
      settingsRepository.updateNotificationCopy(title, body)
      wakeRepository.rescheduleFromLatestWake()
      _state.update { it.copy(message = "Notification copy saved") }
    }
  }

  fun setResetHour(hour: Int) {
    viewModelScope.launch {
      settingsRepository.updateResetHour(hour)
      wakeRepository.onResetHourChanged(hour)
      _state.update { it.copy(message = "Day reset hour set to $hour:00") }
    }
  }

  fun clearMessage() {
    _state.update { it.copy(message = null) }
  }
}
