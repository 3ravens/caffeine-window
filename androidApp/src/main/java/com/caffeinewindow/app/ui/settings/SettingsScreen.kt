package com.caffeinewindow.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caffeinewindow.app.R
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.settings.DelayOption

@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var title by remember(state.settings.notificationTitle) {
    mutableStateOf(state.settings.notificationTitle)
  }
  var body by remember(state.settings.notificationBody) {
    mutableStateOf(state.settings.notificationBody)
  }
  var resetHour by remember(state.settings.resetHour) {
    mutableFloatStateOf(state.settings.resetHour.toFloat())
  }

  var hasRequestedNotifications by remember { mutableStateOf(false) }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) {
    hasRequestedNotifications = true
    viewModel.refreshPermissionStatus()
  }

  LaunchedEffect(Unit) {
    viewModel.refreshPermissionStatus()
  }

  LifecycleResumeEffect(Unit) {
    viewModel.refreshPermissionStatus()
    onPauseOrDispose { }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Text("Settings", style = MaterialTheme.typography.headlineSmall)

    Text("Delay after unlock", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      DelayOption.entries.forEach { option ->
        FilterChip(
          selected = state.settings.delayOption == option,
          onClick = { viewModel.setDelay(option) },
          label = { Text(option.label) },
        )
      }
    }

    Text("Notification copy", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      label = { Text("Title") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = body,
      onValueChange = { body = it },
      label = { Text("Body") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 2,
    )
    Button(
      onClick = { viewModel.updateNotificationCopy(title, body) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Save notification copy")
    }

    Text("Day boundary reset hour", style = MaterialTheme.typography.titleMedium)
    Text("Unlocks before ${resetHour.toInt()}:00 count toward the previous day.")
    Slider(
      value = resetHour,
      onValueChange = { resetHour = it },
      valueRange = 0f..23f,
      steps = 22,
    )
    Button(
      onClick = { viewModel.setResetHour(resetHour.toInt()) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Save reset hour (${resetHour.toInt()}:00)")
    }

    Text("Reliability", style = MaterialTheme.typography.titleMedium)
    PermissionRow(
      label = "Notifications",
      granted = state.permissionStatus.notificationsGranted,
      actionLabel = "Grant",
      onAction = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          context.startActivity(PermissionHelper.createAppNotificationSettingsIntent(context))
          return@PermissionRow
        }
        val activity = context as? android.app.Activity
        val permanentlyDenied = hasRequestedNotifications &&
          activity != null &&
          !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
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
      showAction = true,
    )
    PermissionRow(
      label = "Exact alarms",
      granted = state.permissionStatus.exactAlarmsGranted,
      actionLabel = "Open settings",
      onAction = {
        context.startActivity(PermissionHelper.createExactAlarmSettingsIntent(context))
      },
      showAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
    PermissionRow(
      label = "Battery optimization off",
      granted = state.permissionStatus.batteryOptimizationDisabled,
      actionLabel = "Allow",
      onAction = {
        try {
          context.startActivity(PermissionHelper.createBatteryOptimizationRequestIntent(context))
        } catch (_: Exception) {
          context.startActivity(PermissionHelper.createBatteryOptimizationSettingsIntent())
        }
      },
      showAction = true,
    )

    if (!state.permissionStatus.allReady) {
      Text(
        text = "For reliable overnight wake detection, grant all reliability permissions above.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
      )
    }

    Text(
      stringResource(R.string.settings_monitor_notification_title),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = stringResource(R.string.settings_monitor_notification_body),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.secondary,
    )
    PermissionRow(
      label = stringResource(R.string.monitor_channel_name),
      granted = state.monitorChannelHidden,
      grantedLabel = stringResource(R.string.settings_monitor_notification_hidden),
      needsAttentionLabel = stringResource(R.string.settings_monitor_notification_visible),
      actionLabel = stringResource(R.string.settings_monitor_notification_manage),
      onAction = {
        context.startActivity(PermissionHelper.createMonitorChannelSettingsIntent(context))
      },
      showAction = true,
      alwaysShowAction = true,
    )

    state.message?.let { message ->
      Text(message, style = MaterialTheme.typography.bodySmall)
      LaunchedEffect(message) {
        kotlinx.coroutines.delay(2_000)
        viewModel.clearMessage()
      }
    }
  }
}

@Composable
private fun PermissionRow(
  label: String,
  granted: Boolean,
  actionLabel: String,
  onAction: () -> Unit,
  showAction: Boolean,
  grantedLabel: String = "OK",
  needsAttentionLabel: String = "Needs attention",
  alwaysShowAction: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "$label: ${if (granted) grantedLabel else needsAttentionLabel}",
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f),
    )
    if (showAction && (alwaysShowAction || !granted)) {
      Button(onClick = onAction) {
        Text(actionLabel)
      }
    }
  }
}
