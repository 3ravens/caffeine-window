@file:OptIn(ExperimentalMaterial3Api::class)

package com.caffeinewindow.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.caffeinewindow.app.R
import com.caffeinewindow.app.settings.AppSettings
import com.caffeinewindow.app.settings.DelayOption
import com.caffeinewindow.app.ui.format.formatHourLabel
import com.caffeinewindow.app.ui.theme.CaffeineTheme

/** Callbacks for [SettingsContent] so the content stays stateless and previewable. */
class SettingsActions(
    val onBack: () -> Unit = {},
    val onDelaySelected: (DelayOption) -> Unit = {},
    val onDayStartSelected: (Int) -> Unit = {},
    val onSaveMessage: (String, String) -> Unit = { _, _ -> },
    val onGrantNotifications: () -> Unit = {},
    val onOpenExactAlarms: () -> Unit = {},
    val onOpenBattery: () -> Unit = {},
    val onOpenMonitorChannel: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
fun SettingsContent(
    state: SettingsScreenState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = CaffeineTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { settingsMessageText(it) }
    var showDayStart by rememberSaveable { mutableStateOf(false) }
    var showMessageSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText, duration = SnackbarDuration.Short)
            actions.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg),
            ) {
                SectionHeader(stringResource(R.string.settings_section_timing))
                SettingsCard {
                    DelayRow(
                        current = state.settings.delayOption,
                        onSelect = actions.onDelaySelected,
                    )
                    RowDivider()
                    DayStartRow(
                        currentHour = state.settings.resetHour,
                        onClick = { showDayStart = true },
                    )
                }

                SectionHeader(stringResource(R.string.settings_section_message))
                SettingsCard {
                    MessageRow(
                        title = state.settings.notificationTitle,
                        onClick = { showMessageSheet = true },
                    )
                }

                SectionHeader(stringResource(R.string.settings_section_reliability))
                SettingsCard {
                    ReliabilityRow(
                        label = stringResource(R.string.settings_reliability_notifications),
                        granted = state.permissionStatus.notificationsGranted,
                        statusOn = stringResource(R.string.settings_reliability_on),
                        statusOff = stringResource(R.string.settings_reliability_off_none),
                        leading = {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                            )
                        },
                        onAction = actions.onGrantNotifications,
                    )
                    RowDivider()
                    ReliabilityRow(
                        label = stringResource(R.string.settings_reliability_exact_alarms),
                        granted = state.permissionStatus.exactAlarmsGranted,
                        statusOn = stringResource(R.string.settings_reliability_on),
                        statusOff = stringResource(R.string.settings_reliability_off_alert),
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_alarm),
                                contentDescription = null,
                            )
                        },
                        onAction = actions.onOpenExactAlarms,
                    )
                    RowDivider()
                    ReliabilityRow(
                        label = stringResource(R.string.settings_reliability_battery),
                        granted = state.permissionStatus.batteryOptimizationDisabled,
                        statusOn = stringResource(R.string.settings_reliability_on),
                        statusOff = stringResource(R.string.settings_reliability_off_alert),
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_battery),
                                contentDescription = null,
                            )
                        },
                        onAction = actions.onOpenBattery,
                    )
                    RowDivider()
                    MonitorRow(
                        hidden = state.monitorChannelHidden,
                        onClick = actions.onOpenMonitorChannel,
                    )
                }

                Spacer(Modifier.height(spacing.xl))
            }
        }
    }

    if (showDayStart) {
        DayStartDialog(
            currentHour = state.settings.resetHour,
            onDismiss = { showDayStart = false },
            onConfirm = actions.onDayStartSelected,
        )
    }

    if (showMessageSheet) {
        MessageSheet(
            settings = state.settings,
            onDismiss = { showMessageSheet = false },
            onSave = actions.onSaveMessage,
        )
    }
}

@Composable
private fun settingsMessageText(message: SettingsMessage): String = when (message) {
    is SettingsMessage.DelaySaved -> when {
        message.alreadyOpen -> stringResource(
            R.string.settings_delay_saved_already_open,
            message.minutes,
        )
        message.opensAtClock != null -> stringResource(
            R.string.settings_delay_saved_opens_at,
            message.minutes,
            message.opensAtClock,
        )
        else -> stringResource(R.string.settings_delay_saved, message.minutes)
    }
    SettingsMessage.CopySaved -> stringResource(R.string.settings_message_saved)
    is SettingsMessage.DayStartSaved -> stringResource(
        R.string.settings_day_start_saved,
        message.hour,
    )
}

@Composable
private fun SectionHeader(text: String) {
    val spacing = CaffeineTheme.spacing
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = spacing.xs,
            top = spacing.xl,
            bottom = spacing.sm,
        ),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = CaffeineTheme.spacing.lg),
    )
}

@Composable
private fun DelayRow(current: DelayOption, onSelect: (DelayOption) -> Unit) {
    val spacing = CaffeineTheme.spacing
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.settings_delay_label),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_delay_support),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.md))
        SingleChoiceSegmentedButtonRow {
            DelayOption.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == current,
                    onClick = {
                        if (option != current) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(option)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = DelayOption.entries.size,
                    ),
                ) {
                    Text(stringResource(R.string.settings_delay_option, option.minutes))
                }
            }
        }
    }
}

@Composable
private fun DayStartRow(currentHour: Int, onClick: () -> Unit) {
    val value = formatHourLabel(currentHour)
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_day_start_label)) },
        supportingContent = {
            Text(stringResource(R.string.settings_day_start_support, value))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun MessageRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_message_label)) },
        supportingContent = {
            Text(stringResource(R.string.settings_message_value, title))
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ReliabilityRow(
    label: String,
    granted: Boolean,
    statusOn: String,
    statusOff: String,
    leading: @Composable () -> Unit,
    onAction: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            Text(
                text = if (granted) statusOn else statusOff,
                color = if (granted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingContent = {
            leading()
        },
        trailingContent = {
            if (granted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                TextButton(onClick = onAction) {
                    Text(stringResource(R.string.action_allow))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun MonitorRow(hidden: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_monitor_label)) },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(R.string.settings_monitor_support),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hidden) {
                        stringResource(R.string.settings_monitor_notification_hidden)
                    } else {
                        stringResource(R.string.settings_monitor_notification_visible)
                    },
                    color = if (hidden) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun DayStartDialog(
    currentHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val spacing = CaffeineTheme.spacing
    var selected by rememberSaveable(currentHour) { mutableIntStateOf(currentHour) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_day_start_dialog_title)) },
        text = {
            LazyColumn(
                state = rememberLazyListState(
                    initialFirstVisibleItemIndex = (currentHour - 2).coerceAtLeast(0),
                ),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                items(24) { hour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = hour == selected,
                                role = Role.RadioButton,
                                onClick = { selected = hour },
                            )
                            .padding(vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = hour == selected, onClick = null)
                        Spacer(Modifier.width(spacing.md))
                        Text(
                            text = formatHourLabel(hour),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selected)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun MessageSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val spacing = CaffeineTheme.spacing
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        var title by remember(settings.notificationTitle) {
            mutableStateOf(settings.notificationTitle)
        }
        var body by remember(settings.notificationBody) {
            mutableStateOf(settings.notificationBody)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.settings_message_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(spacing.lg))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.settings_message_title_field)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.md))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.settings_message_body_field)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        title = AppSettings.DEFAULT_NOTIFICATION_TITLE
                        body = AppSettings.DEFAULT_NOTIFICATION_BODY
                    },
                ) {
                    Text(stringResource(R.string.settings_message_reset))
                }
                Button(
                    onClick = {
                        onSave(title, body)
                        onDismiss()
                    },
                    enabled = title.isNotBlank() && body.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
