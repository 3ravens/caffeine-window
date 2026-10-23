package com.caffeinewindow.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.caffeinewindow.app.R
import com.caffeinewindow.app.ui.theme.CaffeineTheme

/**
 * Explains the battery-exemption taps before sending the user to the app's system
 * info page. Android offers no public way to open the per-app battery screen directly,
 * and the one-tap exemption prompt needs a permission Play restricts.
 */
@Composable
fun BatteryGuideDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_guide_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CaffeineTheme.spacing.sm)) {
                Text(
                    text = stringResource(R.string.battery_guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.battery_guide_step_1),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.battery_guide_step_2),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.battery_guide_step_3),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            ) {
                Text(stringResource(R.string.battery_guide_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
