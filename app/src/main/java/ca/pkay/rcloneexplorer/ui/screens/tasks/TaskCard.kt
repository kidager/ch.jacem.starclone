package ca.pkay.rcloneexplorer.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.pkay.rcloneexplorer.Items.SyncDirectionObject
import ca.pkay.rcloneexplorer.Items.Task

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = directionIcon(task.direction),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = directionLabel(task.direction),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = formatPaths(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onOptionsClick) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                )
            }
        }
    }
}

private fun directionIcon(direction: Int) = when (direction) {
    SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
    SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> Icons.Outlined.ArrowUpward
    SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
    SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> Icons.Outlined.ArrowDownward
    SyncDirectionObject.SYNC_BIDIRECTIONAL,
    SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL -> Icons.Outlined.SwapVert
    else -> Icons.Outlined.SwapVert
}

private fun directionLabel(direction: Int) = when (direction) {
    SyncDirectionObject.SYNC_LOCAL_TO_REMOTE -> "Sync: Local \u2192 Remote"
    SyncDirectionObject.SYNC_REMOTE_TO_LOCAL -> "Sync: Remote \u2192 Local"
    SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> "Copy: Local \u2192 Remote"
    SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> "Copy: Remote \u2192 Local"
    SyncDirectionObject.SYNC_BIDIRECTIONAL,
    SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL -> "BiSync"
    else -> "Sync"
}

private fun formatPaths(task: Task): String {
    return when (task.direction) {
        SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
        SyncDirectionObject.COPY_LOCAL_TO_REMOTE ->
            "${task.localPath} \u2192 ${task.remoteId}:${task.remotePath}"
        SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
        SyncDirectionObject.COPY_REMOTE_TO_LOCAL ->
            "${task.remoteId}:${task.remotePath} \u2192 ${task.localPath}"
        else ->
            "${task.remoteId}:${task.remotePath} \u21C4 ${task.localPath}"
    }
}
