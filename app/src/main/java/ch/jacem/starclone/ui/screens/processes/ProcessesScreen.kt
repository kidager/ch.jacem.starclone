package ch.jacem.starclone.ui.screens.processes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.ui.components.StarCloneTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessesScreen(
    viewModel: ProcessesViewModel,
) {
    val selectionMode = viewModel.selectedIds.isNotEmpty()
    val historySelectionMode = viewModel.selectedHistoryIds.isNotEmpty()
    val listState = rememberLazyListState()

    // Auto-scroll to highlighted process
    val highlightedId = viewModel.highlightedProcessId
    LaunchedEffect(highlightedId) {
        if (highlightedId == null) return@LaunchedEffect
        val index = viewModel.activeProcesses.indexOfFirst { it.id == highlightedId }
        if (index >= 0) {
            // +1 to account for the "active_header" item
            listState.animateScrollToItem(index + 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StarCloneTopBar(title = stringResource(R.string.processes))

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize().weight(1f),
    ) {
        when {
            viewModel.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.activeProcesses.isEmpty() && viewModel.history.isEmpty() -> {
                StarCloneEmptyState(
                    icon = Icons.Outlined.Speed,
                    title = stringResource(R.string.no_processes),
                    subtitle = stringResource(R.string.no_processes_subtitle),
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Action bar for active processes
                    if (viewModel.activeProcesses.isNotEmpty()) {
                        item(key = "active_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.active_processes),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                Row {
                                    if (selectionMode) {
                                        FilledTonalButton(onClick = { viewModel.cancelSelected() }) {
                                            Icon(
                                                Icons.Outlined.Cancel,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp),
                                            )
                                            Text(
                                                stringResource(
                                                    R.string.cancel_selected,
                                                    viewModel.selectedIds.size,
                                                ),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { viewModel.clearSelection() }) {
                                            Text(stringResource(android.R.string.cancel))
                                        }
                                    } else if (viewModel.activeProcesses.size > 1) {
                                        OutlinedButton(onClick = { viewModel.cancelAll() }) {
                                            Text(stringResource(R.string.cancel_all))
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = viewModel.activeProcesses,
                            key = { "active_${it.id}" },
                            contentType = { "active" },
                        ) { process ->
                            ProcessCard(
                                process = process,
                                isSelected = process.id in viewModel.selectedIds,
                                selectionMode = selectionMode,
                                isHighlighted = process.id == viewModel.highlightedProcessId,
                                onToggleSelect = { viewModel.toggleSelection(process.id) },
                                onCancel = { viewModel.cancelProcess(process.id) },
                            )
                        }
                    }

                    // History section
                    if (viewModel.history.isNotEmpty()) {
                        item(key = "history_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (viewModel.activeProcesses.isNotEmpty()) 8.dp else 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.transfer_history),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Row {
                                    if (historySelectionMode) {
                                        FilledTonalButton(onClick = { viewModel.deleteSelectedHistory() }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp),
                                            )
                                            Text(
                                                stringResource(
                                                    R.string.delete_selected,
                                                    viewModel.selectedHistoryIds.size,
                                                ),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(onClick = { viewModel.clearHistorySelection() }) {
                                            Text(stringResource(android.R.string.cancel))
                                        }
                                    } else {
                                        TextButton(onClick = { viewModel.clearHistory() }) {
                                            Icon(
                                                Icons.Outlined.DeleteSweep,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 4.dp),
                                            )
                                            Text(stringResource(R.string.clear_history))
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            items = viewModel.history,
                            key = { "history_${it.id}" },
                            contentType = { "history" },
                        ) { record ->
                            HistoryCard(
                                record = record,
                                isSelected = record.id in viewModel.selectedHistoryIds,
                                selectionMode = historySelectionMode,
                                onToggleSelect = { viewModel.toggleHistorySelection(record.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}
