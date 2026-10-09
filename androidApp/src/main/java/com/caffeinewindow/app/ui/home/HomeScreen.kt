package com.caffeinewindow.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caffeinewindow.app.MainActivity
import com.caffeinewindow.app.R
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.wake.WakeEvent
import kotlin.time.Duration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
  onOpenSettings: () -> Unit = {},
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = context as? MainActivity
  var hasRequestedNotifications by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) {
    hasRequestedNotifications = true
    viewModel.refreshPermissionStatus()
  }

  LaunchedEffect(Unit) {
    viewModel.refreshPermissionStatus()
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      !PermissionHelper.areNotificationsGranted(context)
    ) {
      hasRequestedNotifications = true
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  LifecycleResumeEffect(Unit) {
    viewModel.refreshPermissionStatus()
    onPauseOrDispose { }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (!state.permissionStatus.allReady) {
      ReliabilityBanner(
        notificationsGranted = state.permissionStatus.notificationsGranted,
        exactAlarmsGranted = state.permissionStatus.exactAlarmsGranted,
        batteryOptimizationDisabled = state.permissionStatus.batteryOptimizationDisabled,
        onGrantNotifications = {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.startActivity(PermissionHelper.createAppNotificationSettingsIntent(context))
            return@ReliabilityBanner
          }
          val permanentlyDenied = hasRequestedNotifications &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
              activity,
              Manifest.permission.POST_NOTIFICATIONS,
            )
          if (permanentlyDenied) {
            context.startActivity(PermissionHelper.createAppNotificationSettingsIntent(context))
          } else {
            hasRequestedNotifications = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        },
        onOpenExactAlarms = {
          context.startActivity(PermissionHelper.createExactAlarmSettingsIntent(context))
        },
        onOpenBattery = {
          try {
            context.startActivity(PermissionHelper.createBatteryOptimizationRequestIntent(context))
          } catch (_: Exception) {
            context.startActivity(PermissionHelper.createBatteryOptimizationSettingsIntent())
          }
        },
        onOpenSettings = onOpenSettings,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 24.dp),
      )
    }

    when (state.uiState) {
      HomeUiState.IDLE -> {
        Text(
          text = "Waiting for your first unlock",
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Unlock your phone after waking to start today's caffeine delay timer.",
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 12.dp),
        )
      }

      HomeUiState.COUNTDOWN -> {
        Text(
          text = formatDuration(state.remaining),
          style = MaterialTheme.typography.displayMedium,
        )
        Text(
          text = "until your caffeine window opens",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 8.dp),
        )
        Text(
          text = "Delay: ${state.delayMinutes} min after unlock",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(top = 16.dp),
        )
      }

      HomeUiState.WINDOW_OPEN -> {
        Text(
          text = "Window is open",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Your ${state.delayMinutes}-minute delay has passed. You can try caffeine now if you want.",
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 12.dp),
        )
      }
    }

    Text(
      text = state.lastWakeEvent?.let { "Last recorded wake-up: ${formatWakeTime(it)}" }
        ?: "No wake-up recorded yet",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.secondary,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 32.dp),
    )
  }
}

@Composable
private fun ReliabilityBanner(
  notificationsGranted: Boolean,
  exactAlarmsGranted: Boolean,
  batteryOptimizationDisabled: Boolean,
  onGrantNotifications: () -> Unit,
  onOpenExactAlarms: () -> Unit,
  onOpenBattery: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.home_reliability_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      Text(
        text = stringResource(R.string.home_reliability_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      if (!notificationsGranted) {
        Button(onClick = onGrantNotifications, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.home_reliability_grant_notifications))
        }
      }
      if (!exactAlarmsGranted) {
        Button(onClick = onOpenExactAlarms, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.home_reliability_open_exact_alarms))
        }
      }
      if (!batteryOptimizationDisabled) {
        Button(onClick = onOpenBattery, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.home_reliability_open_battery))
        }
      }
      TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_reliability_open_settings))
      }
    }
  }
}

private fun formatWakeTime(wakeEvent: WakeEvent): String {
  val local = wakeEvent.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
  return "%02d:%02d".format(local.hour, local.minute)
}

private fun formatDuration(duration: Duration): String {
  val totalSeconds = duration.inWholeSeconds
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60
  val seconds = totalSeconds % 60
  return if (hours > 0) {
    "%d:%02d:%02d".format(hours, minutes, seconds)
  } else {
    "%02d:%02d".format(minutes, seconds)
  }
}
