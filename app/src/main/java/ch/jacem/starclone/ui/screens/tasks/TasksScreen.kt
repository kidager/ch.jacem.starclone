package ch.jacem.starclone.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneDialog
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.ui.components.StarCloneTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onAddTrigger: () -> Unit,
    onEditTrigger: (Trigger) -> Unit,
) {
    val taskList = viewModel.tasks
    val triggerList = viewModel.triggers
    val isLoading = viewModel.isLoading

    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteTaskDialog by remember { mutableStateOf<Task?>(null) }
    var selectedTrigger by remember { mutableStateOf<Trigger?>(null) }
    var showDeleteTriggerDialog by remember { mutableStateOf<Trigger?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        StarCloneTopBar(title = stringResource(R.string.tasks))

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                taskList.isEmpty() && triggerList.isEmpty() -> {
                    StarCloneEmptyState(
                        icon = Icons.Outlined.SyncAlt,
                        title = stringResource(R.string.tasks_empty_title),
                        subtitle = stringResource(R.string.tasks_empty_subtitle),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Tasks section
                        if (taskList.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.tasks_section_tasks),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            items(
                                items = taskList,
                                key = { "task_${it.id}" },
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    onClick = { onEditTask(task) },
                                    onOptionsClick = { selectedTask = task },
                                )
                            }
                        }

                        // Triggers section
                        item {
                            Text(
                                text = stringResource(R.string.tasks_section_triggers),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            )
                        }

                        if (triggerList.isEmpty()) {
                            item {
                                TextButton(onClick = onAddTrigger) {
                                    Icon(Icons.Outlined.Alarm, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.tasks_add_trigger_hint))
                                }
                            }
                        } else {
                            items(
                                items = triggerList,
                                key = { "trigger_${it.id}" },
                            ) { trigger ->
                                val targetTask = taskList.find { it.id == trigger.triggerTarget }
                                TriggerCard(
                                    trigger = trigger,
                                    isEnabled = trigger.isEnabled,
                                    targetTask = targetTask,
                                    onToggle = { viewModel.toggleTrigger(trigger) },
                                    onOptionsClick = { selectedTrigger = trigger },
                                )
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // FAB with menu
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    FloatingActionButton(
                        onClick = { showFabMenu = true },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tasks_fab_add))
                    }
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_task)) },
                            leadingIcon = { Icon(Icons.Outlined.SyncAlt, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                onAddTask()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_trigger)) },
                            leadingIcon = { Icon(Icons.Outlined.Alarm, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                onAddTrigger()
                            },
                        )
                    }
                }
            }
        }
    }

    // Task options bottom sheet
    selectedTask?.let { task ->
        TaskOptionsSheet(
            task = task,
            onDismiss = { selectedTask = null },
            onStart = {
                viewModel.startTask(task)
                selectedTask = null
            },
            onEdit = {
                onEditTask(task)
                selectedTask = null
            },
            onCopy = {
                viewModel.copyTask(task)
                selectedTask = null
            },
            onDelete = {
                showDeleteTaskDialog = task
                selectedTask = null
            },
        )
    }

    // Trigger options bottom sheet
    selectedTrigger?.let { trigger ->
        TriggerOptionsSheet(
            trigger = trigger,
            onDismiss = { selectedTrigger = null },
            onEdit = {
                onEditTrigger(trigger)
                selectedTrigger = null
            },
            onCopy = {
                viewModel.copyTrigger(trigger)
                selectedTrigger = null
            },
            onDelete = {
                showDeleteTriggerDialog = trigger
                selectedTrigger = null
            },
        )
    }

    // Delete task confirmation
    showDeleteTaskDialog?.let { task ->
        StarCloneDialog(
            onDismissRequest = { showDeleteTaskDialog = null },
            title = stringResource(R.string.delete_task_title),
            text = { Text(stringResource(R.string.delete_task_message, task.title)) },
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteTask(task)
                showDeleteTaskDialog = null
            },
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { showDeleteTaskDialog = null },
            isDestructive = true,
        )
    }

    // Delete trigger confirmation
    showDeleteTriggerDialog?.let { trigger ->
        StarCloneDialog(
            onDismissRequest = { showDeleteTriggerDialog = null },
            title = stringResource(R.string.delete_trigger_title),
            text = { Text(stringResource(R.string.delete_trigger_message, trigger.title)) },
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteTrigger(trigger)
                showDeleteTriggerDialog = null
            },
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { showDeleteTriggerDialog = null },
            isDestructive = true,
        )
    }
}
