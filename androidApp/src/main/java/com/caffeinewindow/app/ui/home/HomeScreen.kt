package com.caffeinewindow.app.ui.home

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
import com.caffeinewindow.app.MainActivity
import com.caffeinewindow.app.permission.PermissionHelper
import com.caffeinewindow.app.ui.components.BatteryGuideDialog

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? MainActivity
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
        // Keep the existing first-launch prompt. A guided onboarding flow is out of scope.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
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

    HomeContent(
        state = state,
        actions = HomeActions(
            onOpenSettings = onOpenSettings,
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
