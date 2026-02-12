package ch.jacem.starclone.ui.screens.fileexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Dialogs.SortDialog
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.Items.SyncDirectionObject
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.ui.components.StarCloneDialog

// --- Sort Dialog ---

@Composable
fun SortDialog(
    currentSortOrder: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    // Derive initial sort by and order from current sort order
    val initialSortBy = when (currentSortOrder) {
        SortDialog.MOD_TIME_DESCENDING, SortDialog.MOD_TIME_ASCENDING -> 1
        SortDialog.SIZE_DESCENDING, SortDialog.SIZE_ASCENDING -> 2
        else -> 0 // name
    }
    val initialAscending = when (currentSortOrder) {
        SortDialog.ALPHA_ASCENDING, SortDialog.MOD_TIME_ASCENDING, SortDialog.SIZE_ASCENDING -> true
        else -> false
    }

    var sortBy by remember { mutableIntStateOf(initialSortBy) }
    var ascending by remember { mutableStateOf(initialAscending) }

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.sort),
        text = {
            Column {
                // Sort by options
                SortRadioOption(
                    label = stringResource(R.string.sort_by_name),
                    selected = sortBy == 0,
                    onClick = { sortBy = 0 },
                )
                SortRadioOption(
                    label = stringResource(R.string.sort_by_date_modified),
                    selected = sortBy == 1,
                    onClick = { sortBy = 1 },
                )
                SortRadioOption(
                    label = stringResource(R.string.sort_by_size),
                    selected = sortBy == 2,
                    onClick = { sortBy = 2 },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Sort order
                SortRadioOption(
                    label = stringResource(R.string.sort_ascending),
                    selected = ascending,
                    onClick = { ascending = true },
                )
                SortRadioOption(
                    label = stringResource(R.string.sort_descending),
                    selected = !ascending,
                    onClick = { ascending = false },
                )
            }
        },
        confirmLabel = stringResource(R.string.ok),
        onConfirm = {
            val order = when (sortBy) {
                1 -> if (ascending) SortDialog.MOD_TIME_ASCENDING else SortDialog.MOD_TIME_DESCENDING
                2 -> if (ascending) SortDialog.SIZE_ASCENDING else SortDialog.SIZE_DESCENDING
                else -> if (ascending) SortDialog.ALPHA_ASCENDING else SortDialog.ALPHA_DESCENDING
            }
            onConfirm(order)
        },
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    )
}

@Composable
private fun SortRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- Input Dialog ---

@Composable
fun InputDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialValue: String = "",
    hint: String = "",
) {
    var text by remember { mutableStateOf(initialValue) }

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = if (hint.isNotEmpty()) {
                        { Text(hint) }
                    } else null,
                    singleLine = true,
                )
            }
        },
        confirmLabel = stringResource(R.string.okay_confirmation),
        onConfirm = { onConfirm(text) },
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    )
}

// --- Delete Confirmation Dialog ---

@Composable
fun DeleteConfirmDialog(
    files: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val title = context.resources.getQuantityString(
        R.plurals.delete_x_items,
        files.size,
        files.size,
    )
    val message = if (files.size == 1) {
        stringResource(R.string.name_will_be_deleted, files[0].name)
    } else null

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = if (message != null) {
            { Text(message) }
        } else null,
        confirmLabel = stringResource(R.string.delete),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

// --- Empty Trash Confirmation ---

@Composable
fun EmptyTrashConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    StarCloneDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.empty_trash_confirmation)) },
        confirmLabel = stringResource(R.string.ok),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

// --- Open As Dialog ---

@Composable
fun OpenAsDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit,
    onText: () -> Unit,
    onAudio: () -> Unit,
    onVideo: () -> Unit,
    onImage: () -> Unit,
) {
    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.open_as),
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                OpenAsButton(
                    label = stringResource(R.string.open_as_text),
                    icon = Icons.Outlined.Description,
                    onClick = onText,
                )
                OpenAsButton(
                    label = stringResource(R.string.open_as_audio),
                    icon = Icons.Outlined.AudioFile,
                    onClick = onAudio,
                )
                OpenAsButton(
                    label = stringResource(R.string.open_as_video),
                    icon = Icons.Outlined.VideoFile,
                    onClick = onVideo,
                )
                OpenAsButton(
                    label = stringResource(R.string.open_as_image),
                    icon = Icons.Outlined.Image,
                    onClick = onImage,
                )
            }
        },
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    )
}

@Composable
private fun OpenAsButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

// --- File Properties Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesSheet(
    fileItem: FileItem,
    remote: RemoteItem,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            PropertyRow(label = stringResource(R.string.path), value = fileItem.path)

            if (!fileItem.isDir) {
                PropertyRow(label = stringResource(R.string.size), value = fileItem.humanReadableSize)
            }

            if (!fileItem.isDir || fileItem.remote.isDirectoryModifiedTimeSupported) {
                PropertyRow(label = stringResource(R.string.modified_time), value = fileItem.formattedModTime)
            }

            if (!fileItem.isDir) {
                PropertyRow(label = stringResource(R.string.type), value = fileItem.mimeType ?: "")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// --- Link Dialog ---

@Composable
fun LinkDialog(
    link: String,
    onDismiss: () -> Unit,
) {
    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.public_link),
        text = {
            Text(
                text = link,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmLabel = stringResource(R.string.ok),
        onConfirm = onDismiss,
    )
}

// --- Cloud Files Dialog ---

@Composable
fun CloudFilesDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.upload_cloud_files_title),
        text = {
            Text(
                text = stringResource(R.string.upload_cloud_files_message, count),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmLabel = stringResource(R.string.upload_cloud_files_confirm),
        onConfirm = onConfirm,
        dismissLabel = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
    )
}

// --- Sync Direction Dialog ---

@Composable
fun SyncDirectionDialog(
    onDismiss: () -> Unit,
    onDirectionSelected: (Int) -> Unit,
) {
    val labels = stringArrayResource(R.array.sync_direction_options)
    val directions = listOf(
        SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
        SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
    )

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.select_sync_direction),
        text = {
            Column {
                labels.forEachIndexed { index, label ->
                    val direction = directions.getOrElse(index) { return@forEachIndexed }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, onClick = {
                                onDirectionSelected(direction)
                            })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    )
}

// --- Serve Dialog ---

@Composable
fun ServeDialog(
    initialAllowRemoteAccess: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (protocol: Int, allowRemoteAccess: Boolean, user: String, password: String) -> Unit,
) {
    var selectedProtocol by remember { mutableIntStateOf(Rclone.SERVE_PROTOCOL_HTTP) }
    var allowRemoteAccess by remember { mutableStateOf(initialAllowRemoteAccess) }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    StarCloneDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.serve_dialog_title),
        text = {
            Column {
                // Protocol selection
                ServeProtocolOption(
                    label = stringResource(R.string.provider_ftp),
                    selected = selectedProtocol == Rclone.SERVE_PROTOCOL_FTP,
                    onClick = { selectedProtocol = Rclone.SERVE_PROTOCOL_FTP },
                )
                ServeProtocolOption(
                    label = stringResource(R.string.serve_dlna),
                    selected = selectedProtocol == Rclone.SERVE_PROTOCOL_DLNA,
                    onClick = { selectedProtocol = Rclone.SERVE_PROTOCOL_DLNA },
                )
                ServeProtocolOption(
                    label = stringResource(R.string.provider_http),
                    selected = selectedProtocol == Rclone.SERVE_PROTOCOL_HTTP,
                    onClick = { selectedProtocol = Rclone.SERVE_PROTOCOL_HTTP },
                )
                ServeProtocolOption(
                    label = stringResource(R.string.provider_webdav),
                    selected = selectedProtocol == Rclone.SERVE_PROTOCOL_WEBDAV,
                    onClick = { selectedProtocol = Rclone.SERVE_PROTOCOL_WEBDAV },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Allow remote access
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = allowRemoteAccess,
                            onClick = { allowRemoteAccess = !allowRemoteAccess },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = allowRemoteAccess, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.allow_remote_access),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Authentication section
                Text(
                    text = stringResource(R.string.authentication),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    supportingText = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    supportingText = { Text(stringResource(R.string.optional)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmLabel = stringResource(R.string.ok),
        onConfirm = { onConfirm(selectedProtocol, allowRemoteAccess, user, password) },
        dismissLabel = stringResource(R.string.cancel),
        onDismiss = onDismiss,
    )
}

@Composable
private fun ServeProtocolOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
