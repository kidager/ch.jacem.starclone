package ca.pkay.rcloneexplorer.ui.screens.logs

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.pkay.rcloneexplorer.util.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogEntry(
    val timestamp: Long,
    val title: String,
    val content: String,
    val type: Int,
)

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application

    var logs by mutableStateOf<List<LogEntry>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonLogs = SyncLog.getLog(context)
            val entries = jsonLogs.mapNotNull { json ->
                try {
                    LogEntry(
                        timestamp = json.getLong(SyncLog.TIMESTAMP),
                        title = json.getString(SyncLog.TITLE),
                        content = json.optString(SyncLog.CONTENT, ""),
                        type = json.getInt(SyncLog.TYPE),
                    )
                } catch (_: Exception) {
                    null
                }
            }
            withContext(Dispatchers.Main) {
                logs = entries
                isLoading = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            SyncLog.delete(context)
            withContext(Dispatchers.Main) {
                logs = emptyList()
            }
        }
    }
}
