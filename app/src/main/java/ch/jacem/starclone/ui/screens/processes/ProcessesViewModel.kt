package ch.jacem.starclone.ui.screens.processes

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.workmanager.EphemeralTaskManager
import ch.jacem.starclone.workmanager.EphemeralWorker
import ch.jacem.starclone.workmanager.SyncManager
import ch.jacem.starclone.workmanager.SyncWorker
import ch.jacem.starclone.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Immutable
data class ProcessInfo(
    val id: String,
    val type: String,
    val state: WorkInfo.State,
    val title: String,
    val percent: Int,
    val speed: String,
    val content: String,
    val fileName: String,
    val remoteName: String,
    val source: String,
    val destination: String,
)

class ProcessesViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "ProcessesViewModel"
    }

    var activeProcesses by mutableStateOf<List<ProcessInfo>>(emptyList())
        private set

    var history by mutableStateOf<List<TransferRecord>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    var selectedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedHistoryIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    var highlightedProcessId by mutableStateOf<String?>(null)
        private set

    init {
        observeActiveProcesses()
        loadHistory()
    }

    private fun observeActiveProcesses() {
        viewModelScope.launch {
            val ephemeralFlow = workManager.getWorkInfosByTagFlow(EphemeralTaskManager.TAG_EPHEMERAL)
            val syncFlow = workManager.getWorkInfosByTagFlow(SyncManager.TAG_SYNC)

            combine(ephemeralFlow, syncFlow) { ephemeral, sync ->
                val ephemeralProcesses = ephemeral
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .map { mapEphemeralWorkInfo(it) }

                val syncProcesses = sync
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .map { mapSyncWorkInfo(it) }

                ephemeralProcesses + syncProcesses
            }.collect { processes ->
                activeProcesses = processes

                // Clean up selections for processes that are no longer active
                val activeIds = processes.map { it.id }.toSet()
                selectedIds = selectedIds.intersect(activeIds)

                // Refresh history when processes finish
                loadHistory()
            }
        }
    }

    private fun mapEphemeralWorkInfo(info: WorkInfo): ProcessInfo {
        val progress = info.progress
        val percent = progress.getInt(EphemeralWorker.PROGRESS_PERCENT, -1)
        val typeStr = progress.getString(EphemeralWorker.PROGRESS_TYPE) ?: ""
        val fileName = progress.getString(EphemeralWorker.PROGRESS_FILENAME) ?: ""
        return ProcessInfo(
            id = info.id.toString(),
            type = typeStr.ifEmpty { "TRANSFER" },
            state = info.state,
            title = fileName.ifEmpty { typeStr },
            percent = percent,
            speed = progress.getString(EphemeralWorker.PROGRESS_SPEED) ?: "",
            content = progress.getString(EphemeralWorker.PROGRESS_CONTENT) ?: "",
            fileName = fileName,
            remoteName = progress.getString(EphemeralWorker.PROGRESS_REMOTE) ?: "",
            source = progress.getString(EphemeralWorker.PROGRESS_SOURCE) ?: "",
            destination = progress.getString(EphemeralWorker.PROGRESS_DEST) ?: "",
        )
    }

    private fun mapSyncWorkInfo(info: WorkInfo): ProcessInfo {
        val progress = info.progress
        val percent = progress.getInt(SyncWorker.PROGRESS_PERCENT, -1)
        val title = progress.getString(SyncWorker.PROGRESS_TITLE) ?: ""
        return ProcessInfo(
            id = info.id.toString(),
            type = "SYNC",
            state = info.state,
            title = title.ifEmpty { "Sync" },
            percent = percent,
            speed = progress.getString(SyncWorker.PROGRESS_SPEED) ?: "",
            content = progress.getString(SyncWorker.PROGRESS_CONTENT) ?: "",
            fileName = progress.getString(SyncWorker.PROGRESS_FILENAME) ?: "",
            remoteName = "",
            source = progress.getString(SyncWorker.PROGRESS_SOURCE) ?: "",
            destination = progress.getString(SyncWorker.PROGRESS_DEST) ?: "",
        )
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                val records = withContext(Dispatchers.IO) {
                    DatabaseHandler(context).getTransferHistory()
                }
                history = records
            } finally {
                delay(300)
                isRefreshing = false
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val records = withContext(Dispatchers.IO) {
                    DatabaseHandler(context).getTransferHistory()
                }
                history = records
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelProcess(id: String) {
        workManager.cancelWorkById(UUID.fromString(id))
    }

    fun cancelSelected() {
        selectedIds.forEach { id ->
            workManager.cancelWorkById(UUID.fromString(id))
        }
        selectedIds = emptySet()
    }

    fun cancelAll() {
        activeProcesses.forEach { process ->
            workManager.cancelWorkById(UUID.fromString(process.id))
        }
        selectedIds = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) {
            selectedIds - id
        } else {
            selectedIds + id
        }
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHandler(context).clearTransferHistory()
            withContext(Dispatchers.Main) {
                history = emptyList()
                selectedHistoryIds = emptySet()
            }
        }
    }

    fun toggleHistorySelection(id: Long) {
        selectedHistoryIds = if (id in selectedHistoryIds) {
            selectedHistoryIds - id
        } else {
            selectedHistoryIds + id
        }
    }

    fun clearHistorySelection() {
        selectedHistoryIds = emptySet()
    }

    fun highlightProcess(workerId: String) {
        highlightedProcessId = workerId
        viewModelScope.launch {
            delay(2000)
            highlightedProcessId = null
        }
    }

    fun deleteSelectedHistory() {
        val ids = selectedHistoryIds.toList()
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseHandler(context).deleteTransferRecords(ids)
            withContext(Dispatchers.Main) {
                history = history.filter { it.id !in selectedHistoryIds }
                selectedHistoryIds = emptySet()
            }
        }
    }
}
