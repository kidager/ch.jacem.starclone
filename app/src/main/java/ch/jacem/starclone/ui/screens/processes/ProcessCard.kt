package ch.jacem.starclone.ui.screens.processes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import ch.jacem.starclone.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProcessCard(
    process: ProcessInfo,
    isSelected: Boolean,
    selectionMode: Boolean,
    isHighlighted: Boolean,
    onToggleSelect: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = processTypeIcon(process.type)
    val typeLabel = processTypeLabel(process.type)
    val isQueued = process.state == WorkInfo.State.ENQUEUED

    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val normalColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val containerColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else normalColor,
        animationSpec = repeatable(
            iterations = 4,
            animation = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "highlight",
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onToggleSelect,
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = process.title.ifEmpty { typeLabel },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isQueued) {
                            stringResource(R.string.process_state_queued)
                        } else {
                            typeLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (process.percent >= 0) {
                    Text(
                        text = "${process.percent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (!isQueued && process.percent >= 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { process.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (isQueued) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Source / destination paths
            if (process.source.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                PathRow(
                    label = stringResource(R.string.process_from),
                    path = process.source,
                )
            }
            if (process.destination.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                PathRow(
                    label = stringResource(R.string.process_to),
                    path = process.destination,
                )
            }

            // Current file being transferred
            if (process.fileName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = process.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Speed and progress details
            if (process.content.isNotEmpty() || process.speed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                val detail = buildString {
                    if (process.content.isNotEmpty()) append(process.content)
                    if (process.speed.isNotEmpty()) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append(process.speed)
                    }
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PathRow(label: String, path: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun processTypeIcon(type: String): ImageVector = when (type.uppercase()) {
    "DOWNLOAD" -> Icons.Outlined.CloudDownload
    "UPLOAD" -> Icons.Outlined.CloudUpload
    "MOVE" -> Icons.Outlined.DriveFileMove
    "DELETE" -> Icons.Outlined.DeleteOutline
    "SYNC" -> Icons.Outlined.SyncAlt
    else -> Icons.Outlined.HourglassTop
}

fun processTypeLabel(type: String): String = when (type.uppercase()) {
    "DOWNLOAD" -> "Download"
    "UPLOAD" -> "Upload"
    "MOVE" -> "Move"
    "DELETE" -> "Delete"
    "SYNC" -> "Sync"
    else -> "Transfer"
}
