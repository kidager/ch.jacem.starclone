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
import org.json.JSONException
import org.json.JSONObject

@Immutable
data class LogEntry(
    val id: Int,
    val timestamp: Long,
    val title: String,
    val content: String,
    val preview: String,
    val type: Int,
    val relativeTime: String,
)

enum class LogFilter(val label: String) {
    ALL("All"),
    ERROR("Errors"),
    INFO("Info"),
}

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application

    var logs by mutableStateOf<List<LogEntry>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var filter by mutableStateOf(LogFilter.ALL)
        private set

    private var allJsonLogs: List<JSONObject> = emptyList()
    private var filteredJsonLogs: List<JSONObject> = emptyList()
    private var loadedCount = 0

    init {
        loadData()
    }

    fun updateFilter(newFilter: LogFilter) {
        if (newFilter == filter) return
        filter = newFilter
        applyFilter()
    }

    private fun applyFilter() {
        viewModelScope.launch {
            filteredJsonLogs = when (filter) {
                LogFilter.ALL -> allJsonLogs
                LogFilter.ERROR -> allJsonLogs.filter {
                    it.optInt(SyncLog.TYPE, -1) == SyncLog.TYPE_ERROR
                }
                LogFilter.INFO -> allJsonLogs.filter {
                    it.optInt(SyncLog.TYPE, -1) == SyncLog.TYPE_INFO
                }
            }
            loadedCount = 0

            val firstPage = withContext(Dispatchers.IO) { parseChunk(0, PAGE_SIZE) }
            loadedCount = firstPage.size
            logs = firstPage
            hasMore = loadedCount < filteredJsonLogs.size
        }
    }

    fun refresh() {
        isRefreshing = true
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val rawLogs = withContext(Dispatchers.IO) { SyncLog.getLog(context) }
                allJsonLogs = rawLogs
                loadedCount = 0

                filteredJsonLogs = when (filter) {
                    LogFilter.ALL -> allJsonLogs
                    LogFilter.ERROR -> allJsonLogs.filter {
                        it.optInt(SyncLog.TYPE, -1) == SyncLog.TYPE_ERROR
                    }
                    LogFilter.INFO -> allJsonLogs.filter {
                        it.optInt(SyncLog.TYPE, -1) == SyncLog.TYPE_INFO
                    }
                }

                val firstPage = withContext(Dispatchers.IO) { parseChunk(0, PAGE_SIZE) }
                loadedCount = firstPage.size
                logs = firstPage
                hasMore = loadedCount < filteredJsonLogs.size
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val nextChunk = withContext(Dispatchers.IO) { parseChunk(loadedCount, PAGE_SIZE) }
                loadedCount += nextChunk.size
                logs = logs + nextChunk
                hasMore = loadedCount < filteredJsonLogs.size
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            SyncLog.delete(context)
            allJsonLogs = emptyList()
            filteredJsonLogs = emptyList()
            loadedCount = 0
            withContext(Dispatchers.Main) {
                logs = emptyList()
                hasMore = false
            }
        }
    }

    private fun parseChunk(offset: Int, count: Int): List<LogEntry> {
        val now = System.currentTimeMillis()
        val end = minOf(offset + count, filteredJsonLogs.size)
        if (offset >= end) return emptyList()

        return filteredJsonLogs.subList(offset, end).mapIndexedNotNull { index, json ->
            try {
                val ts = json.getLong(SyncLog.TIMESTAMP)
                val rawContent = json.optString(SyncLog.CONTENT, "")
                val content = cleanContent(rawContent)
                val preview = content.lineSequence()
                    .firstOrNull { it.isNotEmpty() }
                    ?: ""
                LogEntry(
                    id = offset + index,
                    timestamp = ts,
                    title = json.getString(SyncLog.TITLE),
                    content = content,
                    preview = preview,
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

    private fun cleanContent(raw: String): String {
        if (raw.isBlank()) return raw
        return raw.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@mapNotNull null
                try {
                    val json = JSONObject(trimmed)
                    json.optString("msg", "").ifEmpty { null }
                } catch (_: JSONException) {
                    trimmed
                }
            }
            .distinct()
            .joinToString("\n")
    }

    companion object {
        private const val PAGE_SIZE = 30
    }
}
