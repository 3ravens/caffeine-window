package com.caffeinewindow.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.ui.components.BatteryGuideDialog

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var hasRequestedNotifications by remember { mutableStateOf(false) }
    var showBatteryGuide by rememberSaveable { mutableStateOf(false) }

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

    SettingsContent(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onDelaySelected = viewModel::setDelay,
            onDayStartSelected = viewModel::setResetHour,
            onSaveMessage = viewModel::updateNotificationCopy,
            onGrantNotifications = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    context.startActivity(PermissionHelper.createAppNotificationSettingsIntent(context))
                } else {
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
                }
            },
            onOpenExactAlarms = {
                context.startActivity(PermissionHelper.createExactAlarmSettingsIntent(context))
            },
            onOpenBattery = { showBatteryGuide = true },
            onOpenMonitorChannel = {
                context.startActivity(PermissionHelper.createMonitorChannelSettingsIntent(context))
            },
            onMessageShown = viewModel::clearMessage,
        ),
        modifier = modifier,
    )

    if (showBatteryGuide) {
        BatteryGuideDialog(
            onOpenSettings = { PermissionHelper.openBatteryOptimizationSettings(context) },
            onDismiss = { showBatteryGuide = false },
        )
    }
}
