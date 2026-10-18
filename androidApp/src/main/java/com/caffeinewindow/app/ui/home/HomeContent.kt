@file:OptIn(ExperimentalMaterial3Api::class)

package com.caffeinewindow.app.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caffeinewindow.app.R
import com.caffeinewindow.app.permission.PermissionStatus
import com.caffeinewindow.app.scheduler.SchedulingMath
import com.caffeinewindow.app.ui.components.FittedText
import com.caffeinewindow.app.ui.components.WindowProgressRing
import com.caffeinewindow.app.ui.format.RelativeDay
import com.caffeinewindow.app.ui.format.formatClockTime
import com.caffeinewindow.app.ui.format.wakeLabel
import com.caffeinewindow.app.ui.theme.CaffeineTheme
import com.caffeinewindow.app.ui.theme.extended
import com.caffeinewindow.app.wake.WakeEvent
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** Callbacks for [HomeContent] so the content stays stateless and previewable. */
class HomeActions(
    val onOpenSettings: () -> Unit = {},
    val onGrantNotifications: () -> Unit = {},
    val onOpenExactAlarms: () -> Unit = {},
    val onOpenBattery: () -> Unit = {},
)

private enum class SetupItem(
    @get:StringRes val titleRes: Int,
    @get:StringRes val bodyRes: Int,
) {
    NOTIFICATIONS(R.string.home_setup_notifications_title, R.string.home_setup_notifications_body),
    EXACT_ALARMS(R.string.home_setup_exact_alarms_title, R.string.home_setup_exact_alarms_body),
    BATTERY(R.string.home_setup_battery_title, R.string.home_setup_battery_body),
}

private fun missingSetupItems(status: PermissionStatus): List<SetupItem> = buildList {
    if (!status.notificationsGranted) add(SetupItem.NOTIFICATIONS)
    if (!status.exactAlarmsGranted) add(SetupItem.EXACT_ALARMS)
    if (!status.batteryOptimizationDisabled) add(SetupItem.BATTERY)
}

@Composable
fun HomeContent(
    state: HomeScreenState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val spacing = CaffeineTheme.spacing
    val missing = missingSetupItems(state.permissionStatus)
    var showSetup by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = actions.onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.lg),
        ) {
            if (missing.isNotEmpty()) {
                SetupStatusRow(
                    missingCount = missing.size,
                    onClick = { showSetup = true },
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val ringDiameter = minOf(maxWidth, 280.dp)
                AnimatedContent(
                    targetState = state.uiState,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "homeState",
                    modifier = Modifier.fillMaxSize(),
                ) { uiState ->
                    when (uiState) {
                        HomeUiState.LOADING -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            WindowProgressRing(progress = 0f, diameter = ringDiameter)
                        }
                        HomeUiState.IDLE -> IdleState(state, ringDiameter)
                        HomeUiState.COUNTDOWN -> CountdownState(state, ringDiameter)
                        HomeUiState.WINDOW_OPEN -> WindowOpenState(state, ringDiameter)
                    }
                }
            }

            AssistChip(
                onClick = actions.onOpenSettings,
                label = { Text(stringResource(R.string.home_delay_chip, state.delayMinutes)) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = spacing.xl),
            )
        }
    }

    if (showSetup && missing.isNotEmpty()) {
        SetupSheet(
            items = missing,
            sheetState = sheetState,
            onDismiss = { showSetup = false },
            onAction = { item ->
                when (item) {
                    SetupItem.NOTIFICATIONS -> actions.onGrantNotifications()
                    SetupItem.EXACT_ALARMS -> actions.onOpenExactAlarms()
                    SetupItem.BATTERY -> actions.onOpenBattery()
                }
            },
        )
    }
}

@Composable
private fun IdleState(state: HomeScreenState, diameter: Dp) {
    val spacing = CaffeineTheme.spacing
    val title = stringResource(R.string.home_idle_title)
    val body = stringResource(R.string.home_idle_body)
    val description = "$title. $body"
    val lastWake = lastWakeText(state.lastWakeEvent)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                WindowProgressRing(progress = 0f, diameter = diameter)
                Box(
                    modifier = Modifier.width(diameter * 0.78f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.lg, start = spacing.lg, end = spacing.lg),
            )
        }
        if (lastWake != null) {
            Text(
                text = lastWake,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.sm),
            )
        }
    }
}

@Composable
private fun lastWakeText(event: WakeEvent?): String? {
    if (event == null) return null
    val label = wakeLabel(event.timestamp, Clock.System.now())
    return when (label.day) {
        RelativeDay.TODAY -> stringResource(R.string.home_last_wake_today, label.clock)
        RelativeDay.YESTERDAY -> stringResource(R.string.home_last_wake_yesterday, label.clock)
        RelativeDay.OLDER -> stringResource(
            R.string.home_last_wake_older,
            label.date.orEmpty(),
            label.clock,
        )
    }
}

@Composable
private fun CountdownState(state: HomeScreenState, diameter: Dp) {
    val spacing = CaffeineTheme.spacing
    val wake = state.wakeEvent ?: return
    val target: Instant = SchedulingMath.targetNotificationTime(wake.timestamp, state.delayMinutes)
    val opensAt = formatClockTime(target)
    val wokeAt = formatClockTime(wake.timestamp)
    val minutes = state.remainingMinutes
    val description = pluralStringResource(
        R.plurals.home_countdown_minutes_a11y,
        minutes,
        minutes,
        opensAt,
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                WindowProgressRing(progress = state.elapsedFraction, diameter = diameter)
                Box(
                    modifier = Modifier.width(diameter * 0.78f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FittedText(
                            text = countdownText(minutes),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            text = stringResource(R.string.home_countdown_caption),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.home_times, wokeAt, opensAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.lg),
            )
        }
    }
}

@Composable
private fun WindowOpenState(state: HomeScreenState, diameter: Dp) {
    val spacing = CaffeineTheme.spacing
    val wake = state.wakeEvent ?: return
    val openedAt = formatClockTime(wake.timestamp + state.delayMinutes.minutes)
    val title = stringResource(R.string.home_window_open_title)
    val openedLabel = stringResource(R.string.home_window_opened_at, openedAt)
    val body = stringResource(R.string.home_window_open_body, state.delayMinutes)
    val description = "$title, $openedLabel. $body"

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return@LaunchedEffect
        pulse.animateTo(1.04f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.scale(pulse.value)) {
                WindowProgressRing(
                    progress = 1f,
                    diameter = diameter,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Box(
                modifier = Modifier
                    .width(diameter * 0.78f)
                    .clearAndSetSemantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_coffee),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = spacing.sm),
                    )
                    Text(
                        text = openedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.lg, start = spacing.lg, end = spacing.lg),
        )
    }
}

@Composable
private fun countdownText(minutes: Int): String = when {
    minutes < 60 -> stringResource(R.string.home_countdown_minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.home_countdown_hours, minutes / 60)
    else -> stringResource(
        R.string.home_countdown_hours_minutes,
        minutes / 60,
        minutes % 60,
    )
}

@Composable
private fun SetupStatusRow(
    missingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CaffeineTheme.spacing
    val colors = MaterialTheme.extended
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.warningContainer,
        contentColor = colors.onWarningContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.home_setup_missing_count,
                        missingCount,
                        missingCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SetupSheet(
    items: List<SetupItem>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAction: (SetupItem) -> Unit,
) {
    val spacing = CaffeineTheme.spacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.home_setup_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.home_setup_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.sm),
            )
            Spacer(Modifier.height(spacing.md))
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SetupItemIcon(item)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = spacing.md),
                    ) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(item.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = { onAction(item) }) {
                        Text(stringResource(R.string.action_allow))
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

@Composable
private fun SetupItemIcon(item: SetupItem) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    when (item) {
        SetupItem.NOTIFICATIONS -> Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        SetupItem.EXACT_ALARMS -> Icon(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        SetupItem.BATTERY -> Icon(
            painter = painterResource(R.drawable.ic_battery),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
