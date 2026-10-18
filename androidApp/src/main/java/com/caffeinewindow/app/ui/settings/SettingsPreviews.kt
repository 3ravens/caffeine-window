package com.caffeinewindow.app.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.ui.theme.PreviewTheme

@Preview(name = "Settings light", widthDp = 411, heightDp = 891, showBackground = true)
@Preview(
    name = "Settings dark",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Settings 320 / 2x", widthDp = 320, heightDp = 640, showBackground = true, fontScale = 2f)
@Composable
private fun SettingsContentPreview() {
    PreviewTheme {
        SettingsContent(
            state = SettingsScreenState(
                settings = AppSettings(),
                permissionStatus = PermissionStatus(
                    notificationsGranted = false,
                    exactAlarmsGranted = true,
                    batteryOptimizationDisabled = false,
                ),
                monitorChannelHidden = false,
            ),
            actions = SettingsActions(),
        )
    }
}
