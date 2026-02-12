package ch.jacem.starclone.ui.screens.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.FilterEntry
import ch.jacem.starclone.R
import kotlinx.coroutines.launch

data class FilterEntryState(
    var filterType: Int = FilterEntry.FILTER_EXCLUDE,
    var filter: String = "",
)

@Composable
fun FilterScreen(
    title: String,
    entries: List<FilterEntryState>,
    onSave: (String, List<FilterEntryState>) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var filterTitle by remember { mutableStateOf(title) }
    val filterEntries = remember { mutableStateListOf(*entries.toTypedArray()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (filterTitle.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.filter_data_validation_error_no_title),
                                duration = SnackbarDuration.Short,
                            )
                        }
                        return@FloatingActionButton
                    }
                    onSave(filterTitle, filterEntries.toList())
                },
            ) {
                Icon(Icons.Outlined.Save, contentDescription = "Save")
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                OutlinedTextField(
                    value = filterTitle,
                    onValueChange = { filterTitle = it },
                    label = { Text(stringResource(R.string.filter_edit_hint_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
            }

            itemsIndexed(
                items = filterEntries,
                key = { index, _ -> index },
            ) { index, entry ->
                FilterEntryRow(
                    entry = entry,
                    onTypeChanged = { newType ->
                        filterEntries[index] = entry.copy(filterType = newType)
                    },
                    onFilterChanged = { newFilter ->
                        filterEntries[index] = entry.copy(filter = newFilter)
                    },
                    onDelete = { filterEntries.removeAt(index) },
                )
            }

            item {
                TextButton(
                    onClick = {
                        filterEntries.add(FilterEntryState())
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add filter entry")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FilterEntryRow(
    entry: FilterEntryState,
    onTypeChanged: (Int) -> Unit,
    onFilterChanged: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val includeText = stringResource(R.string.filter_entry_filtertype_include)
    val excludeText = stringResource(R.string.filter_entry_filtertype_exclude)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(0.35f),
        ) {
            OutlinedTextField(
                value = if (entry.filterType == FilterEntry.FILTER_INCLUDE) includeText else excludeText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(includeText) },
                    onClick = {
                        onTypeChanged(FilterEntry.FILTER_INCLUDE)
                        expanded = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(excludeText) },
                    onClick = {
                        onTypeChanged(FilterEntry.FILTER_EXCLUDE)
                        expanded = false
                    },
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = entry.filter,
            onValueChange = onFilterChanged,
            modifier = Modifier.weight(0.55f),
            singleLine = true,
            placeholder = { Text("*.txt") },
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
