package ch.jacem.starclone.ui.screens.remotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneDialog
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.ui.components.StarCloneTopBar
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemotesScreen(
    viewModel: RemotesViewModel,
    onRemoteClick: (RemoteItem) -> Unit,
    onAddRemote: () -> Unit,
    onEditRemote: (RemoteItem) -> Unit,
    onReconnectRemote: (RemoteItem) -> Unit = {},
    onAddToHomeScreen: ((RemoteItem) -> Unit)?,
) {
    var selectedRemote by remember { mutableStateOf<RemoteItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<RemoteItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<RemoteItem?>(null) }
    var showPropertiesDialog by remember { mutableStateOf<RemoteItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        StarCloneTopBar(title = stringResource(R.string.remotes))

    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            !viewModel.isCompatible -> {
                StarCloneEmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Incompatible Architecture",
                    subtitle = "The rclone binary is not compatible with this device.",
                )
            }
            !viewModel.hasConfig -> {
                StarCloneEmptyState(
                    icon = Icons.Outlined.Cloud,
                    title = "No remotes configured",
                    subtitle = "Add a remote to get started",
                    action = {
                        Button(onClick = onAddRemote) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Remote")
                        }
                    },
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = viewModel.remotes,
                        key = { it.name },
                    ) { remote ->
                        RemoteCard(
                            remote = remote,
                            onClick = { onRemoteClick(remote) },
                            onOptionsClick = { selectedRemote = remote },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (!viewModel.isLoading && viewModel.isCompatible) {
            FloatingActionButton(
                onClick = onAddRemote,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Remote")
            }
        }
    }
    }

    // Options bottom sheet
    selectedRemote?.let { remote ->
        RemoteOptionsSheet(
            remote = remote,
            onDismiss = { selectedRemote = null },
            onProperties = {
                showPropertiesDialog = remote
                selectedRemote = null
            },
            onEdit = {
                onEditRemote(remote)
                selectedRemote = null
            },
            onRename = {
                showRenameDialog = remote
                selectedRemote = null
            },
            onPin = {
                if (remote.isPinned) {
                    viewModel.unpinRemote(remote)
                } else {
                    viewModel.pinRemote(remote)
                }
                selectedRemote = null
            },
            onDelete = {
                showDeleteDialog = remote
                selectedRemote = null
            },
            onAddToHomeScreen = onAddToHomeScreen?.let { callback ->
                {
                    callback(remote)
                    selectedRemote = null
                }
            },
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { remote ->
        StarCloneDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = "Delete Remote",
            text = { Text("Are you sure you want to delete \"${remote.displayName}\"?") },
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteRemote(remote)
                showDeleteDialog = null
            },
            dismissLabel = "Cancel",
            onDismiss = { showDeleteDialog = null },
            isDestructive = true,
        )
    }

    // Rename dialog
    showRenameDialog?.let { remote ->
        var newName by remember { mutableStateOf(remote.displayName) }
        StarCloneDialog(
            onDismissRequest = { showRenameDialog = null },
            title = "Rename Remote",
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmLabel = "Save",
            onConfirm = {
                viewModel.renameRemote(remote, newName)
                showRenameDialog = null
            },
            dismissLabel = "Cancel",
            onDismiss = { showRenameDialog = null },
        )
    }

    // Remote properties dialog
    showPropertiesDialog?.let { remote ->
        RemotePropertiesDialog(
            remote = remote,
            onDismiss = { showPropertiesDialog = null },
            onReconnect = onReconnectRemote,
        )
    }
}
