package ch.jacem.starclone.ui.screens.processes

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    record: TransferRecord,
    isSelected: Boolean,
    selectionMode: Boolean,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon = processTypeIcon(record.type)
    val typeLabel = processTypeLabel(record.type)

    val statusColor = when (record.status) {
        TransferRecord.STATUS_SUCCEEDED -> MaterialTheme.colorScheme.primary
        TransferRecord.STATUS_FAILED -> MaterialTheme.colorScheme.error
        TransferRecord.STATUS_CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when (record.status) {
        TransferRecord.STATUS_SUCCEEDED -> stringResource(R.string.process_status_succeeded)
        TransferRecord.STATUS_FAILED -> stringResource(R.string.process_status_failed)
        TransferRecord.STATUS_CANCELLED -> stringResource(R.string.process_status_cancelled)
        else -> record.status
    }

    val formattedSize = remember(record.bytesTransferred) {
        if (record.bytesTransferred > 0) {
            Formatter.formatFileSize(context, record.bytesTransferred)
        } else {
            null
        }
    }

    val formattedDuration = remember(record.startedAt, record.finishedAt) {
        if (record.startedAt > 0 && record.finishedAt > record.startedAt) {
            DateUtils.formatElapsedTime((record.finishedAt - record.startedAt) / 1000)
        } else {
            null
        }
    }

    val timeAgo = remember(record.finishedAt) {
        if (record.finishedAt > 0) {
            DateUtils.getRelativeTimeSpanString(
                record.finishedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
        } else {
            null
        }
    }

    val statusLine = buildString {
        append(statusLabel)
        if (formattedSize != null) append(" \u00b7 $formattedSize")
        if (formattedDuration != null) append(" \u00b7 $formattedDuration")
        if (timeAgo != null) append(" \u00b7 $timeAgo")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onToggleSelect,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                tint = statusColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title.ifEmpty { typeLabel },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val startPadding = if (selectionMode) 68.dp else 36.dp
        if (record.source.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.padding(start = startPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.process_from),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (record.destination.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.padding(start = startPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.process_to),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = record.destination,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
