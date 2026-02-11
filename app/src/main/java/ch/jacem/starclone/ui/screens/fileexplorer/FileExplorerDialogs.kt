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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Dialogs.SortDialog
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort)) },
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
        confirmButton = {
            TextButton(onClick = {
                val order = when (sortBy) {
                    1 -> if (ascending) SortDialog.MOD_TIME_ASCENDING else SortDialog.MOD_TIME_DESCENDING
                    2 -> if (ascending) SortDialog.SIZE_ASCENDING else SortDialog.SIZE_DESCENDING
                    else -> if (ascending) SortDialog.ALPHA_ASCENDING else SortDialog.ALPHA_DESCENDING
                }
                onConfirm(order)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.okay_confirmation))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = if (message != null) {
            { Text(message) }
        } else null,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// --- Empty Trash Confirmation ---

@Composable
fun EmptyTrashConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.empty_trash_confirmation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_as)) },
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.public_link)) },
        text = {
            Text(
                text = link,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
