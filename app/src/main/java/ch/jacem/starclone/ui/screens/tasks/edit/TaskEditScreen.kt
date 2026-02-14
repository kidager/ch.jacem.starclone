package ch.jacem.starclone.ui.screens.tasks.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ch.jacem.starclone.Items.SyncDirectionObject
import ch.jacem.starclone.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    onPickLocalPath: () -> Unit,
    onPickRemotePath: (remoteName: String) -> Unit,
    onCreateFilter: () -> Unit,
    onEditFilter: (filterId: Long) -> Unit,
    onSave: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.validationError) {
        viewModel.validationError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onSave) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = stringResource(R.string.save),
                )
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
            SectionHeader(stringResource(R.string.task_edit_section_general))

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text(stringResource(R.string.task_edit_hint_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Direction
            DirectionDropdown(
                options = viewModel.directionOptions,
                selected = viewModel.direction,
                onSelected = { viewModel.direction = it },
            )

            DirectionDescription(viewModel.direction)

            // Local path
            SectionHeader(stringResource(R.string.task_edit_section_local))

            OutlinedTextField(
                value = viewModel.localPath,
                onValueChange = {},
                label = { Text(stringResource(R.string.task_edit_hint_local)) },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onPickLocalPath) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickLocalPath),
            )

            // Remote
            SectionHeader(stringResource(R.string.task_edit_section_remote))

            RemoteDropdown(
                remoteNames = viewModel.remoteNames,
                selected = viewModel.remoteId,
                onSelected = {
                    viewModel.remoteId = it
                    viewModel.remotePath = ""
                },
            )

            OutlinedTextField(
                value = viewModel.remotePath,
                onValueChange = {},
                label = { Text(stringResource(R.string.task_edit_hint_remote)) },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onPickRemotePath(viewModel.remoteId) }) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onPickRemotePath(viewModel.remoteId) }),
            )

            // Options
            SectionHeader(stringResource(R.string.task_edit_section_options))

            SwitchRow(
                label = stringResource(R.string.transfer_only_on_wifi),
                description = stringResource(R.string.task_hint_wifi_ignore_defaults),
                checked = viewModel.wifiOnly,
                onCheckedChange = { viewModel.wifiOnly = it },
            )

            SwitchRow(
                label = stringResource(R.string.use_md5checksum),
                description = stringResource(R.string.task_hint_md5checksum),
                checked = viewModel.md5sum,
                onCheckedChange = { viewModel.md5sum = it },
            )

            // Filter
            SectionHeader(stringResource(R.string.task_edit_section_filter))

            if (viewModel.filters.isEmpty()) {
                TextButton(onClick = onCreateFilter) {
                    Text(stringResource(R.string.task_edit_create_filter))
                }
            } else {
                FilterDropdown(
                    filters = viewModel.filters,
                    selectedId = viewModel.selectedFilterId,
                    onSelected = { viewModel.selectedFilterId = it },
                    onCreateFilter = onCreateFilter,
                    onEditFilter = onEditFilter,
                    onDeleteFilter = { viewModel.deleteFilter(it) },
                )
            }

            SwitchRow(
                label = stringResource(R.string.task_edit_exclude_delete_toggle_text),
                checked = viewModel.deleteExcluded,
                onCheckedChange = { viewModel.deleteExcluded = it },
            )

            // Follow-up tasks
            SectionHeader(stringResource(R.string.task_edit_section_followup))

            TaskDropdown(
                label = stringResource(R.string.task_edit_on_fail),
                tasks = viewModel.taskList,
                selectedId = viewModel.onFailTaskId,
                onSelected = { viewModel.onFailTaskId = it },
            )

            TaskDropdown(
                label = stringResource(R.string.task_edit_on_success),
                tasks = viewModel.taskList,
                selectedId = viewModel.onSuccessTaskId,
                onSelected = { viewModel.onSuccessTaskId = it },
            )

            Spacer(Modifier.height(80.dp))
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

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionDropdown(
    options: List<DirectionOption>,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.value == selected }?.label ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.task_edit_label_direction)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DirectionDescription(direction: Int) {
    val descriptionRes = when (direction) {
        SyncDirectionObject.SYNC_LOCAL_TO_REMOTE -> R.string.description_sync_direction_sync_toremote
        SyncDirectionObject.SYNC_REMOTE_TO_LOCAL -> R.string.description_sync_direction_sync_tolocal
        SyncDirectionObject.COPY_LOCAL_TO_REMOTE -> R.string.description_sync_direction_copy_toremote
        SyncDirectionObject.COPY_REMOTE_TO_LOCAL -> R.string.description_sync_direction_copy_tolocal
        SyncDirectionObject.SYNC_BIDIRECTIONAL,
        SyncDirectionObject.SYNC_BIDIRECTIONAL_INITIAL -> R.string.description_sync_direction_sync_bidirectional
        else -> R.string.description_sync_direction_sync_toremote
    }
    Text(
        text = stringResource(descriptionRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteDropdown(
    remoteNames: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.task_edit_remote_name)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            remoteNames.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(name)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    filters: List<ch.jacem.starclone.Items.Filter>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
    onCreateFilter: () -> Unit,
    onEditFilter: (Long) -> Unit,
    onDeleteFilter: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noFilterLabel = stringResource(R.string.task_edit_filter_nofilter)
    val selectedLabel = if (selectedId == null) noFilterLabel
    else filters.find { it.id == selectedId }?.title ?: noFilterLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.task_edit_label_filter)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(noFilterLabel) },
                        onClick = {
                            onSelected(null)
                            expanded = false
                        },
                    )
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.title) },
                            onClick = {
                                onSelected(filter.id)
                                expanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.task_edit_create_filter),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        onClick = {
                            expanded = false
                            onCreateFilter()
                        },
                    )
                }
            }
        }

        if (selectedId != null) {
            IconButton(onClick = { onEditFilter(selectedId) }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.task_edit_edit_filter))
            }
            IconButton(onClick = { onDeleteFilter(selectedId) }) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.task_edit_delete_filter))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDropdown(
    label: String,
    tasks: List<ch.jacem.starclone.Items.Task>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.task_edit_none)
    val selectedLabel = if (selectedId == null) noneLabel
    else tasks.find { it.id == selectedId }?.title ?: noneLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
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
