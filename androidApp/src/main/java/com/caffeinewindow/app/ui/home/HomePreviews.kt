package com.caffeinewindow.app.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.ui.theme.PreviewTheme
import com.caffeinewindow.app.wake.WakeEvent
import kotlinx.datetime.Instant

private val WakeToday = WakeEvent(timestamp = Instant.parse("2026-10-17T07:12:00Z"), source = "screen_unlock")
private val WakeYesterday = WakeEvent(timestamp = Instant.parse("2026-10-16T07:04:00Z"), source = "screen_unlock")

private val FullySetUp = PermissionStatus(
    notificationsGranted = true,
    exactAlarmsGranted = true,
    batteryOptimizationDisabled = true,
)

private val NeedsSetup = PermissionStatus(
    notificationsGranted = false,
    exactAlarmsGranted = true,
    batteryOptimizationDisabled = false,
)

private val previewStates = listOf(
    HomeScreenState(uiState = HomeUiState.LOADING),
    HomeScreenState(
        uiState = HomeUiState.IDLE,
        lastWakeEvent = WakeYesterday,
        permissionStatus = FullySetUp,
    ),
    HomeScreenState(
        uiState = HomeUiState.COUNTDOWN,
        wakeEvent = WakeToday,
        lastWakeEvent = WakeToday,
        delayMinutes = 90,
        remainingMinutes = 47,
        elapsedFraction = 0.48f,
        permissionStatus = FullySetUp,
    ),
    HomeScreenState(
        uiState = HomeUiState.WINDOW_OPEN,
        wakeEvent = WakeToday,
        lastWakeEvent = WakeToday,
        delayMinutes = 90,
        remainingMinutes = 0,
        elapsedFraction = 1f,
        permissionStatus = FullySetUp,
    ),
    HomeScreenState(
        uiState = HomeUiState.COUNTDOWN,
        wakeEvent = WakeToday,
        lastWakeEvent = WakeToday,
        delayMinutes = 90,
        remainingMinutes = 47,
        elapsedFraction = 0.48f,
        permissionStatus = NeedsSetup,
    ),
)

internal class HomeStateProvider : PreviewParameterProvider<HomeScreenState> {
    override val values: Sequence<HomeScreenState> get() = previewStates.asSequence()
}

@Preview(name = "Home light", widthDp = 411, heightDp = 891, showBackground = true)
@Preview(
    name = "Home dark",
    widthDp = 411,
    heightDp = 891,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Home 320 / 2x", widthDp = 320, heightDp = 640, showBackground = true, fontScale = 2f)
@Composable
private fun HomeContentPreview(
    @PreviewParameter(HomeStateProvider::class) state: HomeScreenState,
) {
    PreviewTheme {
        HomeContent(state = state, actions = HomeActions())
    }
}
