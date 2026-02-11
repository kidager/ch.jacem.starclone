package ch.jacem.starclone.ui.screens.logs

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.jacem.starclone.util.SyncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Immutable
data class LogEntry(
    val id: Int,
    val timestamp: Long,
    val title: String,
    val content: String,
    val type: Int,
    val relativeTime: String,
)

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application

    var logs by mutableStateOf<List<LogEntry>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set

    private var allJsonLogs: List<JSONObject> = emptyList()
    private var loadedCount = 0

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val rawLogs = SyncLog.getLog(context)
            allJsonLogs = rawLogs
            loadedCount = 0

            val firstPage = parseChunk(0, PAGE_SIZE)
            loadedCount = firstPage.size

            withContext(Dispatchers.Main) {
                logs = firstPage
                hasMore = loadedCount < allJsonLogs.size
                isLoading = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true

        viewModelScope.launch(Dispatchers.IO) {
            val nextChunk = parseChunk(loadedCount, PAGE_SIZE)
            loadedCount += nextChunk.size

            withContext(Dispatchers.Main) {
                logs = logs + nextChunk
                hasMore = loadedCount < allJsonLogs.size
                isLoadingMore = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            SyncLog.delete(context)
            allJsonLogs = emptyList()
            loadedCount = 0
            withContext(Dispatchers.Main) {
                logs = emptyList()
                hasMore = false
            }
        }
    }

    private fun parseChunk(offset: Int, count: Int): List<LogEntry> {
        val now = System.currentTimeMillis()
        val end = minOf(offset + count, allJsonLogs.size)
        if (offset >= end) return emptyList()

        return allJsonLogs.subList(offset, end).mapIndexedNotNull { index, json ->
            try {
                val ts = json.getLong(SyncLog.TIMESTAMP)
                LogEntry(
                    id = offset + index,
                    timestamp = ts,
                    title = json.getString(SyncLog.TITLE),
                    content = json.optString(SyncLog.CONTENT, ""),
                    type = json.getInt(SyncLog.TYPE),
                    relativeTime = DateUtils.getRelativeTimeSpanString(
                        ts, now, DateUtils.MINUTE_IN_MILLIS,
                    ).toString(),
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
