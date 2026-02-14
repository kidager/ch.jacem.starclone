package ch.jacem.starclone.ui.screens.tasks.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TriggerEditScreen(
    viewModel: TriggerEditViewModel,
    onSave: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.validationError) {
        viewModel.validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onSave) {
                Icon(Icons.Outlined.Save, contentDescription = stringResource(R.string.save))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // General section
            SectionHeader(stringResource(R.string.trigger_edit_section_general))

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text(stringResource(R.string.trigger_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = viewModel.isEnabled,
                    onCheckedChange = { viewModel.isEnabled = it },
                )
            }

            TypeDropdown(
                selected = viewModel.type,
                onSelected = { viewModel.type = it },
            )

            // Schedule section
            AnimatedVisibility(visible = viewModel.type == Trigger.TRIGGER_TYPE_SCHEDULE) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader(stringResource(R.string.trigger_edit_section_schedule))

                    val dayLabels = listOf(
                        stringResource(R.string.day_chip_mon),
                        stringResource(R.string.day_chip_tue),
                        stringResource(R.string.day_chip_wed),
                        stringResource(R.string.day_chip_thu),
                        stringResource(R.string.day_chip_fri),
                        stringResource(R.string.day_chip_sat),
                        stringResource(R.string.day_chip_sun),
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dayLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = viewModel.isWeekdayEnabled(index),
                                onClick = {
                                    viewModel.updateWeekday(index, !viewModel.isWeekdayEnabled(index))
                                },
                                label = { Text(label) },
                            )
                        }
                    }

                    val formattedTime = remember(viewModel.timeHour, viewModel.timeMinute) {
                        String.format("%02d:%02d", viewModel.timeHour, viewModel.timeMinute)
                    }

                    val timeInteractionSource = remember { MutableInteractionSource() }
                    LaunchedEffect(timeInteractionSource) {
                        timeInteractionSource.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                showTimePicker = true
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formattedTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.time)) },
                        trailingIcon = {
                            Icon(Icons.Outlined.AccessTime, contentDescription = null)
                        },
                        interactionSource = timeInteractionSource,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Interval section
            AnimatedVisibility(visible = viewModel.type == Trigger.TRIGGER_TYPE_INTERVAL) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionHeader(stringResource(R.string.trigger_edit_section_interval))

                    IntervalDropdown(
                        selected = viewModel.intervalMinutes,
                        onSelected = { viewModel.intervalMinutes = it },
                    )
                }
            }

            // Target task
            SectionHeader(stringResource(R.string.trigger_edit_section_target))

            if (viewModel.taskList.isEmpty()) {
                Text(
                    text = stringResource(R.string.trigger_edit_no_tasks_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                TargetTaskDropdown(
                    tasks = viewModel.taskList,
                    selectedId = viewModel.targetTaskId,
                    onSelected = { viewModel.targetTaskId = it },
                )
            }

            Spacer(Modifier.height(80.dp))
        }

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = viewModel.timeHour,
                initialMinute = viewModel.timeMinute,
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.timeHour = timePickerState.hour
                        viewModel.timeMinute = timePickerState.minute
                        showTimePicker = false
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                text = { TimePicker(state = timePickerState) },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheduleLabel = stringResource(R.string.trigger_type_schedule)
    val intervalLabel = stringResource(R.string.trigger_type_interval)
    val selectedLabel = if (selected == Trigger.TRIGGER_TYPE_SCHEDULE) scheduleLabel else intervalLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(scheduleLabel) },
                onClick = {
                    onSelected(Trigger.TRIGGER_TYPE_SCHEDULE)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text(intervalLabel) },
                onClick = {
                    onSelected(Trigger.TRIGGER_TYPE_INTERVAL)
                    expanded = false
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalDropdown(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        15 to stringResource(R.string.trigger_interval_15_min),
        30 to stringResource(R.string.trigger_interval_30_min),
        60 to stringResource(R.string.trigger_interval_60_min),
        120 to stringResource(R.string.trigger_interval_120_min),
    )
    val selectedLabel = options.find { it.first == selected }?.second
        ?: options.find { it.first == 60 }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.task_interval)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetTaskDropdown(
    tasks: List<ch.jacem.starclone.Items.Task>,
    selectedId: Long,
    onSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = tasks.find { it.id == selectedId }?.title ?: tasks.firstOrNull()?.title ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.task_to_run)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = { Text(task.title) },
                    onClick = {
                        onSelected(task.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
