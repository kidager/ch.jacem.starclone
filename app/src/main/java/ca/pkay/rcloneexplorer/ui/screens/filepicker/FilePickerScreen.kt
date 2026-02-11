package ca.pkay.rcloneexplorer.ui.screens.filepicker

import android.text.format.DateUtils
import android.text.format.Formatter
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
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.ui.components.StarCloneEmptyState
import java.io.File

@Composable
fun FilePickerScreen(
    files: List<File>,
    currentPath: String,
    rootPath: String,
    isDestinationMode: Boolean,
    selectedFiles: Set<String>,
    showFab: Boolean,
    onDirectoryClick: (File) -> Unit,
    onFileClick: (File) -> Unit,
    onSelectionToggle: (File) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onFabClick: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onFabClick) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.select),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Breadcrumbs(
                currentPath = currentPath,
                rootPath = rootPath,
                onClick = onBreadcrumbClick,
            )

            if (files.isEmpty()) {
                StarCloneEmptyState(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.empty_folder),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = files,
                        key = { it.absolutePath },
                    ) { file ->
                        FilePickerItem(
                            file = file,
                            isDestinationMode = isDestinationMode,
                            isSelected = selectedFiles.contains(file.absolutePath),
                            onDirectoryClick = onDirectoryClick,
                            onFileClick = onFileClick,
                            onSelectionToggle = onSelectionToggle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(
    currentPath: String,
    rootPath: String,
    onClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val relativePath = if (currentPath.startsWith(rootPath)) {
        currentPath.removePrefix(rootPath)
    } else {
        currentPath
    }
    val segments = relativePath.split("/").filter { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "/",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onClick(rootPath) }
                .padding(horizontal = 4.dp),
        )

        var builtPath = rootPath
        segments.forEach { segment ->
            builtPath = "$builtPath/$segment"
            val targetPath = builtPath
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = segment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onClick(targetPath) }
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun FilePickerItem(
    file: File,
    isDestinationMode: Boolean,
    isSelected: Boolean,
    onDirectoryClick: (File) -> Unit,
    onFileClick: (File) -> Unit,
    onSelectionToggle: (File) -> Unit,
) {
    val isDir = file.isDirectory
    val dimmed = isDestinationMode && !isDir

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.5f else 1f)
            .clickable(enabled = !dimmed) {
                if (isDir) {
                    onDirectoryClick(file)
                } else if (!isDestinationMode) {
                    onSelectionToggle(file)
                } else {
                    onFileClick(file)
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isDir) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isDir) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                val context = LocalContext.current
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        file.lastModified(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                    ).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isDir) {
                    Text(
                        text = " · ${Formatter.formatFileSize(context, file.length())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!isDestinationMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectionToggle(file) },
            )
        }
    }
}
