package ch.jacem.starclone.ui.screens.fileexplorer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.workmanager.Type

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    onFileClick: (FileItem) -> Unit,
    onUploadFiles: () -> Unit,
    onDownloadFiles: (List<FileItem>) -> Unit,
    onSyncSelected: (String) -> Unit,
    onServe: () -> Unit,
    onShowGoToDialog: () -> Unit,
    onShowServeDialog: () -> Unit,
) {
    val context = LocalContext.current
    val remote = viewModel.remote

    var showFabMenu by remember { mutableStateOf(false) }
    var fileForOptions by remember { mutableStateOf<FileItem?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var deleteTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var openAsTarget by remember { mutableStateOf<FileItem?>(null) }
    var showFileProperties by remember { mutableStateOf<FileItem?>(null) }

    // Link result dialog
    val linkResult = viewModel.linkResult

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            if (viewModel.isInSearchMode) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.exitSearchMode() }) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                )
            }

            // Breadcrumb bar
            BreadcrumbBar(
                breadcrumbs = viewModel.breadcrumbs,
                onCrumbClick = { path -> viewModel.navigateToBreadcrumb(path) },
            )

            // Transfer progress
            if (viewModel.activeTransfers.isNotEmpty()) {
                TransferProgressBanner(viewModel.activeTransfers)
            }

            // Main content
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                val allFiles = viewModel.files

                when {
                    !viewModel.isLoading && allFiles.isEmpty() && viewModel.searchQuery.isNotEmpty() -> {
                        StarCloneEmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = stringResource(R.string.no_search_results),
                        )
                    }
                    !viewModel.isLoading && allFiles.isEmpty() -> {
                        StarCloneEmptyState(
                            icon = Icons.Outlined.FolderOpen,
                            title = stringResource(R.string.empty_folder),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp),
                        ) {
                            items(
                                items = allFiles,
                                key = { it.path },
                            ) { fileItem ->
                                FileListItem(
                                    fileItem = fileItem,
                                    isSelected = fileItem in viewModel.selectedFiles,
                                    isInSelectMode = viewModel.isInSelectMode,
                                    isInMoveMode = viewModel.isInMoveMode,
                                    onClick = {
                                        when {
                                            viewModel.isInSelectMode -> viewModel.toggleFileSelection(fileItem)
                                            viewModel.isInMoveMode && fileItem.isDir -> viewModel.navigateToDirectory(fileItem)
                                            fileItem.isDir -> viewModel.navigateToDirectory(fileItem)
                                            else -> onFileClick(fileItem)
                                        }
                                    },
                                    onLongClick = {
                                        if (!viewModel.isInMoveMode) {
                                            viewModel.toggleFileSelection(fileItem)
                                        }
                                    },
                                    onOptionsClick = { fileForOptions = fileItem },
                                )
                            }
                        }
                    }
                }
            }

            // Selection bottom bar
            if (viewModel.isInSelectMode && !viewModel.isInMoveMode) {
                SelectionBottomBar(
                    selectedCount = viewModel.selectedFiles.size,
                    canRename = viewModel.selectedFiles.size == 1,
                    showDownload = !remote.isRemoteType(RemoteItem.LOCAL, RemoteItem.SAFW) && !remote.isPathAlias,
                    onDownload = {
                        onDownloadFiles(viewModel.selectedFiles.toList())
                    },
                    onMove = { viewModel.enterMoveMode(viewModel.selectedFiles.toList()) },
                    onRename = {
                        renameTarget = viewModel.selectedFiles.first()
                    },
                    onDelete = {
                        deleteTargets = viewModel.selectedFiles.toList()
                    },
                )
            }

            // Move mode bottom bar
            if (viewModel.isInMoveMode) {
                MoveBottomBar(
                    onNewFolder = { showNewFolderDialog = true },
                    onCancel = { viewModel.cancelMove() },
                    onSelect = { viewModel.confirmMove() },
                )
            }
        }

        // FAB (only when not in select/move mode)
        if (!viewModel.isInSelectMode && !viewModel.isInMoveMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            onUploadFiles()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = stringResource(R.string.fab_upload_files))
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            showNewFolderDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = stringResource(R.string.fab_new_folder))
                    }
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fab_new_folder))
                }
            }
        }
    }

    // --- Dialogs ---

    if (showSortDialog) {
        SortDialog(
            currentSortOrder = viewModel.sortOrder,
            onDismiss = { showSortDialog = false },
            onConfirm = { newOrder ->
                viewModel.updateSortOrder(newOrder)
                showSortDialog = false
            },
        )
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

    renameTarget?.let { target ->
        InputDialog(
            title = stringResource(R.string.rename_file),
            message = stringResource(R.string.type_new_file_name),
            initialValue = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameFile(target, newName)
                renameTarget = null
            },
        )
    }

    deleteTargets?.let { targets ->
        DeleteConfirmDialog(
            files = targets,
            onDismiss = { deleteTargets = null },
            onConfirm = {
                viewModel.deleteFiles(targets)
                deleteTargets = null
            },
        )
    }

    if (showEmptyTrashConfirm) {
        EmptyTrashConfirmDialog(
            onDismiss = { showEmptyTrashConfirm = false },
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashConfirm = false
            },
        )
    }

    openAsTarget?.let { target ->
        OpenAsDialog(
            fileItem = target,
            onDismiss = { openAsTarget = null },
            onText = {
                onFileClick(target) // Fragment handles the open-as mode
                openAsTarget = null
            },
            onAudio = {
                onFileClick(target)
                openAsTarget = null
            },
            onVideo = {
                onFileClick(target)
                openAsTarget = null
            },
            onImage = {
                onFileClick(target)
                openAsTarget = null
            },
        )
    }

    showFileProperties?.let { fileItem ->
        FilePropertiesSheet(
            fileItem = fileItem,
            remote = remote,
            onDismiss = { showFileProperties = null },
        )
    }

    linkResult?.let { link ->
        LinkDialog(
            link = link,
            onDismiss = { viewModel.consumeLinkResult() },
        )
    }

    // File options bottom sheet
    fileForOptions?.let { fileItem ->
        FileOptionsSheet(
            fileItem = fileItem,
            remote = remote,
            onDismiss = { fileForOptions = null },
            onProperties = {
                showFileProperties = fileItem
                fileForOptions = null
            },
            onOpenAs = {
                openAsTarget = fileItem
                fileForOptions = null
            },
            onDownload = {
                onDownloadFiles(listOf(fileItem))
                fileForOptions = null
            },
            onMove = {
                viewModel.enterMoveMode(listOf(fileItem))
                fileForOptions = null
            },
            onRename = {
                renameTarget = fileItem
                fileForOptions = null
            },
            onDelete = {
                deleteTargets = listOf(fileItem)
                fileForOptions = null
            },
            onLink = {
                viewModel.generateLink(fileItem.path)
                fileForOptions = null
            },
            onSync = {
                onSyncSelected(fileItem.path)
                fileForOptions = null
            },
        )
    }
}

// --- Top-level actions (called from Fragment toolbar) ---

data class FileExplorerMenuActions(
    val onSort: () -> Unit,
    val onSelectAll: () -> Unit,
    val onSearch: () -> Unit,
    val onServe: () -> Unit,
    val onEmptyTrash: () -> Unit,
    val onLink: () -> Unit,
    val onSync: () -> Unit,
    val onGoTo: () -> Unit,
)

// --- Breadcrumb Bar ---

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onCrumbClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(breadcrumbs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        breadcrumbs.forEachIndexed { index, (label, path) ->
            val isLast = index == breadcrumbs.lastIndex
            if (isLast) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onCrumbClick(path) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// --- Transfer Progress Banner ---

@Composable
private fun TransferProgressBanner(transfers: List<TransferProgress>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (transfer in transfers) {
            TransferProgressCard(transfer)
        }
    }
}

@Composable
private fun TransferProgressCard(transfer: TransferProgress) {
    val typeLabel = when (transfer.type) {
        Type.UPLOAD -> stringResource(R.string.fab_upload_files)
        Type.DOWNLOAD -> stringResource(R.string.download)
        Type.MOVE -> stringResource(R.string.move)
        Type.DELETE -> stringResource(R.string.delete)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "${transfer.percent}%",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (transfer.fileName.isNotEmpty()) {
                Text(
                    text = transfer.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { transfer.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            if (transfer.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (transfer.speed.isNotEmpty()) "${transfer.content} \u00B7 ${transfer.speed}" else transfer.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --- File List Item ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    isInSelectMode: Boolean,
    isInMoveMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    ListItem(
        headlineContent = {
            Text(
                text = fileItem.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Row {
                Text(
                    text = fileItem.humanReadableModTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!fileItem.isDir) {
                    Text(
                        text = " \u00B7 ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = fileItem.humanReadableSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        leadingContent = {
            if (isInSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                )
            } else {
                Icon(
                    imageVector = if (fileItem.isDir) Icons.Outlined.Folder else Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (fileItem.isDir) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailingContent = if (!isInMoveMode) {
            {
                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.properties),
                    )
                }
            }
        } else null,
        colors = ListItemDefaults.colors(containerColor = containerColor),
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    )
}

// --- Selection Bottom Bar ---

@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    canRename: Boolean,
    showDownload: Boolean,
    onDownload: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDownload) {
            IconButton(onClick = onDownload) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.download))
                    Text(stringResource(R.string.download), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        IconButton(onClick = onMove) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.DriveFileMove, contentDescription = stringResource(R.string.move))
                Text(stringResource(R.string.move), style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onRename, enabled = canRename) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.DriveFileRenameOutline,
                    contentDescription = stringResource(R.string.rename),
                    tint = if (canRename) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
                Text(stringResource(R.string.rename), style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onDelete) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// --- Move Bottom Bar ---

@Composable
private fun MoveBottomBar(
    onNewFolder: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNewFolder) {
            Icon(Icons.Filled.CreateNewFolder, contentDescription = stringResource(R.string.new_folder))
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel))
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onSelect) {
            Text(stringResource(R.string.select))
        }
    }
}

// --- File Options Bottom Sheet ---

@Composable
fun FileOptionsSheet(
    fileItem: FileItem,
    remote: RemoteItem,
    onDismiss: () -> Unit,
    onProperties: () -> Unit,
    onOpenAs: () -> Unit,
    onDownload: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLink: () -> Unit,
    onSync: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val actions = buildList {
        add(
            ch.jacem.starclone.ui.components.BottomSheetAction(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.properties),
                onClick = onProperties,
            ),
        )
        if (!fileItem.isDir) {
            add(
                ch.jacem.starclone.ui.components.BottomSheetAction(
                    icon = Icons.Outlined.Description,
                    label = stringResource(R.string.open_as),
                    onClick = onOpenAs,
                ),
            )
        }
        if (!remote.isRemoteType(RemoteItem.LOCAL, RemoteItem.SAFW) && !remote.isPathAlias) {
            add(
                ch.jacem.starclone.ui.components.BottomSheetAction(
                    icon = Icons.Outlined.Download,
                    label = stringResource(R.string.download),
                    onClick = onDownload,
                ),
            )
        }
        add(
            ch.jacem.starclone.ui.components.BottomSheetAction(
                icon = Icons.Outlined.DriveFileMove,
                label = stringResource(R.string.move),
                onClick = onMove,
            ),
        )
        add(
            ch.jacem.starclone.ui.components.BottomSheetAction(
                icon = Icons.Outlined.DriveFileRenameOutline,
                label = stringResource(R.string.rename),
                onClick = onRename,
            ),
        )
        if (remote.hasLinkSupport()) {
            add(
                ch.jacem.starclone.ui.components.BottomSheetAction(
                    icon = Icons.Outlined.Link,
                    label = stringResource(R.string.link),
                    onClick = onLink,
                ),
            )
        }
        if (fileItem.isDir && remote.hasSyncSupport()) {
            add(
                ch.jacem.starclone.ui.components.BottomSheetAction(
                    icon = Icons.Outlined.Sync,
                    label = stringResource(R.string.sync),
                    onClick = onSync,
                ),
            )
        }
        add(
            ch.jacem.starclone.ui.components.BottomSheetAction(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.delete),
                tint = errorColor,
                onClick = onDelete,
            ),
        )
    }

    ch.jacem.starclone.ui.components.StarCloneBottomSheet(
        title = fileItem.name,
        actions = actions,
        onDismiss = onDismiss,
    )
}
