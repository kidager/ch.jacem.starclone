package ch.jacem.starclone.ui.screens.settings.logging

import android.os.Process
import android.widget.Toast
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
import ch.jacem.starclone.ui.components.StarCloneDialog
import ch.jacem.starclone.ui.components.StarCloneListItem
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun LoggingSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val logsKey = stringResource(R.string.pref_key_logs)

    var logsEnabled by remember {
        mutableStateOf(prefs.getBoolean(logsKey, false))
    }
    var showSigquitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "Logging",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.use_logs),
                supportingText = stringResource(R.string.use_logs_summary),
                trailingContent = {
                    Switch(
                        checked = logsEnabled,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    logsEnabled = !logsEnabled
                    prefs.edit().putBoolean(logsKey, logsEnabled).apply()
                },
            )
        }
        item {
            Text(
                text = "Rclone",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_send_sigquit_title),
                supportingText = stringResource(R.string.pref_send_sigquit_summary),
                onClick = { showSigquitDialog = true },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showSigquitDialog) {
        StarCloneDialog(
            onDismissRequest = { showSigquitDialog = false },
            title = stringResource(R.string.pref_send_sigquit_title),
            text = { Text("This will kill all rclone processes and close the app. Continue?") },
            confirmLabel = "Kill",
            onConfirm = {
                Toast.makeText(context, "StarClone: Stopping everything", Toast.LENGTH_SHORT).show()
                sigquitAll()
            },
            dismissLabel = "Cancel",
            onDismiss = { showSigquitDialog = false },
            isDestructive = true,
        )
    }
}

private fun sigquitAll() {
    try {
        val process = Runtime.getRuntime().exec("ps")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val regex = Regex("\\s+(\\d+)\\s+\\d+\\s+\\d+\\s+.+librclone.+$")
        reader.lineSequence().forEach { line ->
            regex.find(line)?.let { match ->
                val pid = match.groupValues[1].toIntOrNull()
                if (pid != null) {
                    Process.sendSignal(pid, Process.SIGNAL_QUIT)
                }
            }
        }
        reader.close()
    } catch (_: Exception) {
    }
    Process.killProcess(Process.myPid())
}
