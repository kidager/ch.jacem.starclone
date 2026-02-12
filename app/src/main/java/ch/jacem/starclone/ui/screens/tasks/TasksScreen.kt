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
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.ui.components.StarCloneDialog
import ch.jacem.starclone.ui.components.StarCloneEmptyState

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onAddTrigger: () -> Unit,
    onEditTrigger: (Trigger) -> Unit,
) {
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showDeleteTaskDialog by remember { mutableStateOf<Task?>(null) }
    var selectedTrigger by remember { mutableStateOf<Trigger?>(null) }
    var showDeleteTriggerDialog by remember { mutableStateOf<Trigger?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            viewModel.tasks.isEmpty() && viewModel.triggers.isEmpty() -> {
                StarCloneEmptyState(
                    icon = Icons.Outlined.SyncAlt,
                    title = "No tasks configured",
                    subtitle = "Tap + to create a task or trigger",
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Tasks section
                    if (viewModel.tasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Tasks",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(
                            items = viewModel.tasks,
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
                            text = "Triggers",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }

                    if (viewModel.triggers.isEmpty()) {
                        item {
                            TextButton(onClick = onAddTrigger) {
                                Icon(Icons.Outlined.Alarm, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add a trigger to automate tasks")
                            }
                        }
                    } else {
                        items(
                            items = viewModel.triggers,
                            key = { "trigger_${it.id}_${it.isEnabled}" },
                        ) { trigger ->
                            val targetTask = viewModel.tasks.find { it.id == trigger.triggerTarget }
                            TriggerCard(
                                trigger = trigger,
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
        if (!viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Task") },
                        leadingIcon = { Icon(Icons.Outlined.SyncAlt, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            onAddTask()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Add Trigger") },
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
            title = "Delete Task",
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTask(task)
                showDeleteTaskDialog = null
            },
            dismissLabel = "Cancel",
            onDismiss = { showDeleteTaskDialog = null },
            isDestructive = true,
        )
    }

    // Delete trigger confirmation
    showDeleteTriggerDialog?.let { trigger ->
        StarCloneDialog(
            onDismissRequest = { showDeleteTriggerDialog = null },
            title = "Delete Trigger",
            text = { Text("Are you sure you want to delete \"${trigger.title}\"?") },
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTrigger(trigger)
                showDeleteTriggerDialog = null
            },
            dismissLabel = "Cancel",
            onDismiss = { showDeleteTriggerDialog = null },
            isDestructive = true,
        )
    }
}
