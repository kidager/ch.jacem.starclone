package ch.jacem.starclone.ui.screens.logs

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import ch.jacem.starclone.ui.components.StarCloneDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.ui.components.StarCloneEmptyState
import ch.jacem.starclone.util.SyncLog
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(viewModel: LogsViewModel) {
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<LogEntry?>(null) }
    val listState = rememberLazyListState()

    // Trigger loadMore when scrolling near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 5 && viewModel.hasMore && !viewModel.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky toolbar: filter dropdown + clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogFilterDropdown(
                selected = viewModel.filter,
                onSelected = { viewModel.updateFilter(it) },
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { showClearDialog = true },
                enabled = viewModel.logs.isNotEmpty(),
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Clear logs")
            }
        }

        // Content
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().weight(1f),
        ) {
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                viewModel.logs.isEmpty() -> {
                    StarCloneEmptyState(
                        icon = Icons.Outlined.History,
                        title = "No logs yet",
                        subtitle = if (viewModel.filter == LogFilter.ALL) {
                            "Sync activity will appear here"
                        } else {
                            "No ${viewModel.filter.label.lowercase()} logs found"
                        },
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = viewModel.logs,
                            key = { it.id },
                            contentType = { "log" },
                        ) { entry ->
                            LogCard(
                                entry = entry,
                                onClick = { selectedEntry = entry },
                            )
                        }

                        if (viewModel.isLoadingMore) {
                            item(key = "loading_more", contentType = "loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // Clear logs confirmation
    if (showClearDialog) {
        StarCloneDialog(
            onDismissRequest = { showClearDialog = false },
            title = "Clear Logs",
            text = { Text("Are you sure you want to delete all logs?") },
            confirmLabel = "Clear",
            onConfirm = {
                viewModel.clearLogs()
                showClearDialog = false
            },
            dismissLabel = "Cancel",
            onDismiss = { showClearDialog = false },
            isDestructive = true,
        )
    }

    // Log detail dialog
    selectedEntry?.let { entry ->
        val context = LocalContext.current
        val fullDate = remember(entry.timestamp) {
            val dateFormat = DateFormat.getDateFormat(context)
            val timeFormat = DateFormat.getTimeFormat(context)
            val date = Date(entry.timestamp)
            "${dateFormat.format(date)} ${timeFormat.format(date)}"
        }
        val typeLabel = if (entry.type == SyncLog.TYPE_ERROR) "Error" else "Info"

        StarCloneDialog(
            onDismissRequest = { selectedEntry = null },
            title = entry.title,
            text = {
                Text("$typeLabel \u2022 $fullDate\n\n${entry.content}")
            },
            confirmLabel = "Close",
            onConfirm = { selectedEntry = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogFilterDropdown(
    selected: LogFilter,
    onSelected: (LogFilter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .width(140.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LogFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onSelected(filter)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
