package ch.jacem.starclone.ui.screens.folderpicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.ui.screens.fileexplorer.FileExplorerViewModel
import ch.jacem.starclone.ui.screens.fileexplorer.InputDialog

@Composable
fun FolderPickerScreen(
    viewModel: FileExplorerViewModel,
    onSelectFolder: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Breadcrumbs
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                viewModel.breadcrumbs.forEachIndexed { index, (label, path) ->
                    if (index > 0) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index == viewModel.breadcrumbs.lastIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable { viewModel.navigateToBreadcrumb(path) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }

            // File list
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                viewModel.files.isEmpty() && viewModel.error == null -> {
                    StarCloneEmptyState(
                        icon = Icons.Outlined.Folder,
                        title = stringResource(R.string.empty_folder),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = viewModel.files,
                            contentType = { if (it.isDir) "dir" else "file" },
                        ) { fileItem ->
                            val isDir = fileItem.isDir
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = fileItem.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (isDir) Icons.Outlined.Folder else Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = if (isDir) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                },
                                modifier = Modifier.clickable(enabled = isDir) {
                                    viewModel.navigateToDirectory(fileItem)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }
        }

        // FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmallFloatingActionButton(
                onClick = { showNewFolderDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(Icons.Outlined.CreateNewFolder, contentDescription = stringResource(R.string.create_new_folder))
            }
            FloatingActionButton(
                onClick = {
                    var path = "/" + viewModel.currentPath
                    if (path.startsWith("///")) {
                        path = path.replace("//", "")
                    }
                    onSelectFolder(path)
                },
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.select))
            }
        }
    }

    if (showNewFolderDialog) {
        InputDialog(
            title = stringResource(R.string.create_new_folder),
            message = stringResource(R.string.type_new_folder_name),
            hint = stringResource(R.string.hint_new_folder),
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                viewModel.createDirectory(name)
                showNewFolderDialog = false
            },
        )
    }
}
