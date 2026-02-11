package ch.jacem.starclone.ui.screens.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.Trigger

@Composable
fun TriggerCard(
    trigger: Trigger,
    targetTask: Task?,
    onToggle: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = trigger.isEnabled,
                onCheckedChange = { onToggle() },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = trigger.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (trigger.type == Trigger.TRIGGER_TYPE_SCHEDULE) {
                    Text(
                        text = formatSchedule(trigger, is24Hour),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatWeekdays(trigger),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Every ${trigger.time} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                targetTask?.let {
                    Text(
                        text = "Task: ${it.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onOptionsClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Options")
            }
        }
    }
}

private fun formatSchedule(trigger: Trigger, is24Hour: Boolean): String {
    val totalMinutes = trigger.time
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (is24Hour) {
        String.format("%02d:%02d", hours, minutes)
    } else {
        val displayHour = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        val amPm = if (hours < 12) "AM" else "PM"
        String.format("%d:%02d %s", displayHour, minutes, amPm)
    }
}

private val DAY_LABELS = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun formatWeekdays(trigger: Trigger): String {
    val days = (0..6).filter { trigger.isEnabledAtDay(it) }.map { DAY_LABELS[it] }
    return if (days.size == 7) "Every day" else days.joinToString(", ")
}
