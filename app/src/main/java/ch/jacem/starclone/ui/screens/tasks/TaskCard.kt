package ch.jacem.starclone.ui.screens.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.SyncDirectionObject
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptionsClick,
            ),
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
                    contentDescription = stringResource(R.string.task_options),
                )
            }
        }
    }
}

private fun directionIcon(direction: Int): ImageVector = when (direction) {
    SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
    SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> Icons.Outlined.ArrowUpward
    SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
    SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> Icons.Outlined.ArrowDownward
    SyncDirectionObject.SYNC_BIDIRECTIONAL,
    SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL -> Icons.Outlined.SwapVert
    SyncDirectionObject.SYNC_REMOTE_TO_REMOTE,
    SyncDirectionObject.COPY_REMOTE_TO_REMOTE -> Icons.Outlined.SwapHoriz
    else -> Icons.Outlined.SwapVert
}

@Composable
private fun directionLabel(direction: Int): String = when (direction) {
    SyncDirectionObject.SYNC_LOCAL_TO_REMOTE -> stringResource(R.string.task_direction_sync_to_remote)
    SyncDirectionObject.SYNC_REMOTE_TO_LOCAL -> stringResource(R.string.task_direction_sync_to_local)
    SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> stringResource(R.string.task_direction_copy_to_remote)
    SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> stringResource(R.string.task_direction_copy_to_local)
    SyncDirectionObject.SYNC_BIDIRECTIONAL,
    SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL -> stringResource(R.string.task_direction_bisync)
    SyncDirectionObject.SYNC_REMOTE_TO_REMOTE -> stringResource(R.string.task_direction_sync_remote_to_remote)
    SyncDirectionObject.COPY_REMOTE_TO_REMOTE -> stringResource(R.string.task_direction_copy_remote_to_remote)
    else -> stringResource(R.string.task_direction_sync)
}

private fun formatPaths(task: Task): String {
    return when (task.direction) {
        SyncDirectionObject.SYNC_LOCAL_TO_REMOTE,
        SyncDirectionObject.COPY_LOCAL_TO_REMOTE ->
            "${task.localPath} \u2192 ${task.remoteId}:${task.remotePath}"
        SyncDirectionObject.SYNC_REMOTE_TO_LOCAL,
        SyncDirectionObject.COPY_REMOTE_TO_LOCAL ->
            "${task.remoteId}:${task.remotePath} \u2192 ${task.localPath}"
        SyncDirectionObject.SYNC_REMOTE_TO_REMOTE,
        SyncDirectionObject.COPY_REMOTE_TO_REMOTE ->
            "${task.remoteId}:${task.remotePath} \u2192 ${task.remoteId2}:${task.remotePath2}"
        else ->
            "${task.remoteId}:${task.remotePath} \u21C4 ${task.localPath}"
    }
}
