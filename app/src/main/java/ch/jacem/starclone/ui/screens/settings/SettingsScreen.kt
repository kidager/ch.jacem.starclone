package ch.jacem.starclone.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.BuildConfig
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.ui.components.StarCloneListItem

@Composable
fun SettingsScreen(
    onCategoryClick: (Int) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.settings_header_general),
                supportingText = stringResource(R.string.generalSettingsDescription),
                leadingIcon = Icons.Outlined.Settings,
                leadingIconTint = Color(0xFFC71414),
                onClick = { onCategoryClick(GENERAL_SETTINGS) },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.file_access),
                supportingText = stringResource(R.string.fileAccessSettingsDescription),
                leadingIcon = Icons.Outlined.Folder,
                leadingIconTint = Color(0xFF3584D8),
                onClick = { onCategoryClick(FILE_ACCESS_SETTINGS) },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.look_and_feel),
                supportingText = stringResource(R.string.theminSettingsDescription),
                leadingIcon = Icons.Outlined.Palette,
                leadingIconTint = Color(0xFF43AB3B),
                onClick = { onCategoryClick(LOOK_AND_FEEL_SETTINGS) },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.notifications_pref_title),
                supportingText = stringResource(R.string.notificationSettingsDescription),
                leadingIcon = Icons.Outlined.Notifications,
                leadingIconTint = Color(0xFFE59308),
                onClick = { onCategoryClick(NOTIFICATION_SETTINGS) },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.import_rclone_config),
                supportingText = stringResource(R.string.importSettingsDescription),
                leadingIcon = Icons.Outlined.FileDownload,
                leadingIconTint = Color(0xFF2A9702),
                onClick = onImport,
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.export_rclone_config),
                supportingText = stringResource(R.string.exportSettingsDescription),
                leadingIcon = Icons.Outlined.FileUpload,
                leadingIconTint = Color(0xFF2A9702),
                onClick = onExport,
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.logging_settings_header),
                supportingText = stringResource(R.string.loggingSettingsDescription),
                leadingIcon = Icons.Outlined.BugReport,
                leadingIconTint = Color(0xFF32D6E5),
                onClick = { onCategoryClick(LOGGING_SETTINGS) },
            )
        }
        item {
            val context = LocalContext.current
            val rcloneVersion = remember { Rclone(context).rcloneVersion }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "rclone $rcloneVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

const val GENERAL_SETTINGS = 1
const val FILE_ACCESS_SETTINGS = 2
const val LOOK_AND_FEEL_SETTINGS = 3
const val LOGGING_SETTINGS = 4
const val NOTIFICATION_SETTINGS = 5
