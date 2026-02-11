package ch.jacem.starclone.ui.screens.settings.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneListItem
import ch.jacem.starclone.extract.updates.UpdateChecker

@Composable
fun NotificationSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val updatesKey = stringResource(R.string.pref_key_app_updates)
    val reportsKey = stringResource(R.string.pref_key_app_notification_reports)

    var updatesEnabled by remember {
        mutableStateOf(prefs.getBoolean(updatesKey, false))
    }
    var reportsEnabled by remember {
        mutableStateOf(prefs.getBoolean(reportsKey, false))
    }
    val isManagedInstallation = remember { UpdateChecker(context).isManagedInstallation() }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (!isManagedInstallation) {
            item {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.app_update_title),
                    supportingText = stringResource(R.string.app_updates_notifications),
                    trailingContent = {
                        Switch(
                            checked = updatesEnabled,
                            onCheckedChange = null,
                        )
                    },
                    onClick = {
                        updatesEnabled = !updatesEnabled
                        prefs.edit().putBoolean(updatesKey, updatesEnabled).apply()
                        UpdateChecker(context).schedule()
                    },
                )
            }
        }

        item {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            StarCloneListItem(
                headlineText = "Sync Reports",
                supportingText = stringResource(R.string.notification_reports),
                trailingContent = {
                    Switch(
                        checked = reportsEnabled,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    reportsEnabled = !reportsEnabled
                    prefs.edit().putBoolean(reportsKey, reportsEnabled).apply()
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.open_notification_settings_title),
                supportingText = stringResource(R.string.open_notification_settings_summary),
                onClick = {
                    val intent = Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                        putExtra("app_package", context.packageName)
                        putExtra("app_uid", context.applicationInfo.uid)
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
