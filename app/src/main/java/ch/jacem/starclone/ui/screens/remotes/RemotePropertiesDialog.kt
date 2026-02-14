package ch.jacem.starclone.ui.screens.remotes

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.ui.components.StarCloneDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface StorageState {
    data object Loading : StorageState
    data object Failed : StorageState
    data class Loaded(val used: Long, val total: Long, val free: Long) : StorageState
}

@Composable
fun RemotePropertiesDialog(
    remote: RemoteItem,
    onDismiss: () -> Unit,
    onReconnect: (RemoteItem) -> Unit,
) {
    val context = LocalContext.current
    val rclone = remember { Rclone(context) }
    var storageState by remember { mutableStateOf<StorageState>(StorageState.Loading) }

    LaunchedEffect(remote.name) {
        storageState = StorageState.Loading
        val result = withContext(Dispatchers.IO) {
            rclone.aboutRemote(remote)
        }
        storageState = if (result.hasFailed()) {
            StorageState.Failed
        } else {
            StorageState.Loaded(result.used, result.total, result.free)
        }
    }

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = remote.displayName ?: remote.name,
        text = {
            Column {
                // Remote type
                Text(
                    text = remote.typeReadable,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Storage usage
                Text(
                    text = stringResource(R.string.remote_properties_storage_usage),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = storageState) {
                    is StorageState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    is StorageState.Failed -> {
                        Text(
                            text = stringResource(R.string.remote_properties_about_failed),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is StorageState.Loaded -> {
                        val used = Formatter.formatFileSize(context, state.used)
                        val statsText = if (state.total < 0 || state.free < 0) {
                            stringResource(R.string.remote_properties_storage_used, used)
                        } else {
                            val total = Formatter.formatFileSize(context, state.total)
                            val free = Formatter.formatFileSize(context, state.free)
                            stringResource(R.string.remote_properties_storage_stats, used, total, free)
                        }
                        Text(
                            text = statsText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (state.total > 0 && state.used >= 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (state.used.toFloat() / state.total).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Reconnect button for OAuth remotes
                if (remote.isOAuth) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.remote_properties_authorization),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onReconnect(remote)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.remote_properties_authorize_tap))
                    }
                }
            }
        },
        confirmLabel = stringResource(R.string.ok),
        onConfirm = onDismiss,
    )
}
