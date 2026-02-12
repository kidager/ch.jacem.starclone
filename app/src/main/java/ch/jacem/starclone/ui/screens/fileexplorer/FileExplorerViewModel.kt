package ch.jacem.starclone.ui.screens.fileexplorer

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import ch.jacem.starclone.Dialogs.SortDialog
import ch.jacem.starclone.FileComparators
import ch.jacem.starclone.Items.DirectoryObject
import ch.jacem.starclone.Items.FileItem
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.util.FLog
import ch.jacem.starclone.workmanager.EphemeralTaskManager
import ch.jacem.starclone.workmanager.EphemeralWorker
import ch.jacem.starclone.workmanager.Type
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application
    private val rclone = Rclone(application)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val directoryObject = DirectoryObject()
    private val pathStack = ArrayDeque<String>()

    lateinit var remote: RemoteItem
        private set
    var remoteName: String = ""
        private set
    var startAtRoot: Boolean = false

    // UI state
    var files by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var currentPath by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sortOrder by mutableIntStateOf(SortDialog.ALPHA_ASCENDING)
        private set
    var showSortDialog by mutableStateOf(false)

    // Selection state
    var selectedFiles by mutableStateOf<Set<FileItem>>(emptySet())
        private set
    val isInSelectMode: Boolean get() = selectedFiles.isNotEmpty()

    // Move mode
    var isInMoveMode by mutableStateOf(false)
        private set
    var moveFiles by mutableStateOf<List<FileItem>>(emptyList())
        private set
    var moveStartPath: String? = null
        private set

    // Dialog state
    var showCloudFilesDialog by mutableStateOf<Int?>(null)
    var showSyncDirectionDialog by mutableStateOf(false)
    var showServeDialog by mutableStateOf(false)
    var syncRemotePath: String? = null

    // Search
    var isInSearchMode by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set

    // Breadcrumb segments: list of (label, path)
    var breadcrumbs by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    // Transfer progress
    var activeTransfers by mutableStateOf<List<TransferProgress>>(emptyList())
        private set

    private var fetchJob: Job? = null
    private var initialized = false

    fun initialize(remoteItem: RemoteItem) {
        if (initialized) return
        initialized = true
        remote = remoteItem
        remoteName = remoteItem.name
        sortOrder = prefs.getInt(SHARED_PREFS_SORT_ORDER, SortDialog.ALPHA_ASCENDING)

        val rootPath = "//$remoteName"
        currentPath = rootPath
        directoryObject.setPath(rootPath)
        breadcrumbs = listOf(Pair(remoteItem.displayName ?: remoteName, rootPath))
        fetchDirectory()
        observeTransferProgress()
    }

    private fun observeTransferProgress() {
        viewModelScope.launch {
            var previousRunningIds = emptySet<String>()
            WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(EphemeralTaskManager.TAG_EPHEMERAL)
                .collect { workInfos ->
                    val runningInfos = workInfos.filter { it.state == WorkInfo.State.RUNNING }
                    val currentRunningIds = runningInfos.map { it.id.toString() }.toSet()

                    // Refresh file list when any transfer finishes
                    if (previousRunningIds.isNotEmpty() && previousRunningIds.any { it !in currentRunningIds }) {
                        fetchDirectory(silent = true)
                    }
                    previousRunningIds = currentRunningIds

                    activeTransfers = runningInfos.mapNotNull { info ->
                        val progress = info.progress
                        val percent = progress.getInt(EphemeralWorker.PROGRESS_PERCENT, -1)
                        if (percent < 0) return@mapNotNull null
                        val typeStr = progress.getString(EphemeralWorker.PROGRESS_TYPE) ?: ""
                        val type = try { Type.valueOf(typeStr) } catch (_: Exception) { return@mapNotNull null }
                        TransferProgress(
                            id = info.id.toString(),
                            type = type,
                            percent = percent,
                            content = progress.getString(EphemeralWorker.PROGRESS_CONTENT) ?: "",
                            speed = progress.getString(EphemeralWorker.PROGRESS_SPEED) ?: "",
                            fileName = progress.getString(EphemeralWorker.PROGRESS_FILENAME) ?: "",
                        )
                    }
                }
        }
    }

    fun fetchDirectory(silent: Boolean = false, refresh: Boolean = false) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (refresh) isRefreshing = true
            else if (!silent) isLoading = true
            error = null

            val result = withContext(Dispatchers.IO) {
                rclone.getDirectoryContent(remote, directoryObject.currentPath, startAtRoot)
            }

            isLoading = false
            isRefreshing = false

            if (result == null) {
                val rcloneError = rclone.lastError
                error = if (!rcloneError.isNullOrBlank()) {
                    rcloneError
                } else {
                    context.getString(R.string.error_getting_dir_content)
                }
                return@launch
            }

            directoryObject.setContent(result)
            sortDirectoryContent()
            applyFilesToUI()
        }
    }

    fun navigateToDirectory(fileItem: FileItem) {
        pathStack.addLast(directoryObject.currentPath)

        if (isInSearchMode) exitSearchMode()

        val path = fileItem.path
        breadcrumbs = breadcrumbs + Pair(fileItem.name, path)

        if (!directoryObject.isContentValid(path) && directoryObject.isPathInCache(path)) {
            directoryObject.restoreFromCache(path)
            sortDirectoryContent()
            applyFilesToUI()
            fetchDirectory(silent = true)
        } else if (directoryObject.isPathInCache(path)) {
            directoryObject.restoreFromCache(path)
            sortDirectoryContent()
            applyFilesToUI()
        } else {
            directoryObject.setPath(path)
            currentPath = path
            files = emptyList()
            fetchDirectory()
        }
        currentPath = directoryObject.currentPath
    }

    fun navigateUp(): Boolean {
        if (isInSelectMode) {
            clearSelection()
            return true
        }
        if (isInSearchMode) {
            exitSearchMode()
            return true
        }
        if (pathStack.isEmpty()) return false

        fetchJob?.cancel()
        isLoading = false

        val path = pathStack.removeLast()
        breadcrumbs = breadcrumbs.dropLast(1)

        restoreOrFetch(path)
        return true
    }

    fun navigateToBreadcrumb(path: String) {
        if (directoryObject.currentPath == path) return
        if (isInSearchMode) exitSearchMode()

        fetchJob?.cancel()
        isLoading = false

        // Pop stack until we find the path
        while (pathStack.isNotEmpty() && pathStack.last() != path) {
            pathStack.removeLast()
        }
        if (pathStack.isNotEmpty()) pathStack.removeLast()

        // Trim breadcrumbs to the selected path
        val index = breadcrumbs.indexOfFirst { it.second == path }
        if (index >= 0) {
            breadcrumbs = breadcrumbs.take(index + 1)
        }

        restoreOrFetch(path)
    }

    fun refresh() {
        fetchDirectory(refresh = files.isNotEmpty())
    }

    fun consumeError() {
        error = null
    }

    // --- File operations ---

    fun createDirectory(name: String) {
        if (name.isBlank()) return
        val newDir = if (directoryObject.currentPath == "//$remoteName") {
            name
        } else {
            "${directoryObject.currentPath}/$name"
        }
        val pathWhenStarted = directoryObject.currentPath
        isLoading = true

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                rclone.makeDirectory(remote, newDir)
            }

            if (directoryObject.currentPath != pathWhenStarted) {
                directoryObject.removePathFromCache(pathWhenStarted)
                isLoading = false
                return@launch
            }
            isLoading = false
            if (!result) {
                error = context.getString(R.string.error_mkdir)
                return@launch
            }
            fetchDirectory(silent = true)
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        if (fileItem.name == newName) return
        clearSelection()

        val newFilePath = if (directoryObject.currentPath == "//$remoteName") {
            newName
        } else {
            "${directoryObject.currentPath}/$newName"
        }
        val pathWhenStarted = directoryObject.currentPath
        isLoading = true

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                rclone.moveTo(remote, fileItem.path, newFilePath)
            }

            if (directoryObject.currentPath != pathWhenStarted) {
                directoryObject.removePathFromCache(pathWhenStarted)
                isLoading = false
                return@launch
            }
            isLoading = false
            if (!result) {
                error = context.getString(R.string.error_moving_file)
                return@launch
            }
            fetchDirectory(silent = true)
        }
    }

    fun deleteFiles(filesToDelete: List<FileItem>) {
        clearSelection()
        for (item in filesToDelete) {
            EphemeralTaskManager.queueDelete(context, remote, item, directoryObject.currentPath)
        }
    }

    fun enterMoveMode(filesToMove: List<FileItem>) {
        if (filesToMove.isEmpty()) return
        moveStartPath = directoryObject.currentPath
        moveFiles = filesToMove.toList()
        clearSelection()
        isInMoveMode = true
    }

    fun confirmMove() {
        if (moveFiles.isEmpty()) {
            error = context.getString(R.string.error_moving_file)
            cancelMove()
            return
        }
        val firstFile = moveFiles[0]
        val oldPath = firstFile.path
        val index = oldPath.lastIndexOf(firstFile.name)
        val path2 = if (index > 0) oldPath.substring(0, index - 1) else "//$remoteName"

        for (moveItem in moveFiles) {
            EphemeralTaskManager.queueMove(context, remote, directoryObject.currentPath, moveItem, path2)
        }
        moveFiles = emptyList()
        moveStartPath = null
        isInMoveMode = false
    }

    fun cancelMove() {
        isInMoveMode = false
        val startPath = moveStartPath
        moveFiles = emptyList()
        moveStartPath = null

        if (startPath != null && startPath != directoryObject.currentPath) {
            // Rebuild breadcrumbs and stack for the start path
            buildStackFromPath(startPath)
            rebuildBreadcrumbs()
            restoreOrFetch(startPath)
        }
    }

    fun generateLink(path: String) {
        isLoading = true
        viewModelScope.launch {
            val link = withContext(Dispatchers.IO) {
                rclone.link(remote, path)
            }
            isLoading = false
            if (link == null) {
                error = context.getString(R.string.error_generating_link)
                return@launch
            }
            // Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Copied link", link))
            _linkResult.value = link
        }
    }

    // One-shot event for link dialog
    private val _linkResult = mutableStateOf<String?>(null)
    var linkResult by _linkResult
        private set

    fun consumeLinkResult() {
        linkResult = null
    }

    fun emptyTrash() {
        isLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                rclone.emptyTrashCan(remoteName)
            }
            isLoading = false
            if (!result) {
                error = context.getString(R.string.error_emptying_trash)
            }
        }
    }

    // --- Selection ---

    fun toggleFileSelection(fileItem: FileItem) {
        selectedFiles = if (fileItem in selectedFiles) {
            selectedFiles - fileItem
        } else {
            selectedFiles + fileItem
        }
    }

    fun selectAll() {
        selectedFiles = files.toSet()
    }

    fun clearSelection() {
        selectedFiles = emptySet()
    }

    // --- Search ---

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applyFilesToUI()
    }

    fun enterSearchMode() {
        isInSearchMode = true
    }

    fun exitSearchMode() {
        isInSearchMode = false
        searchQuery = ""
        applyFilesToUI()
    }

    // --- Sort ---

    fun updateSortOrder(newSortOrder: Int) {
        sortOrder = newSortOrder
        if (sortOrder > 0) {
            prefs.edit().putInt(SHARED_PREFS_SORT_ORDER, sortOrder).apply()
        }
        if (!directoryObject.isDirectoryContentEmpty) {
            sortDirectoryContent()
            applyFilesToUI()
        }
    }

    // --- Go To (SFTP) ---

    fun goToRoot(setAsDefault: Boolean) {
        startAtRoot = true
        directoryObject.clear()
        val path = "//$remoteName"
        directoryObject.setPath(path)
        currentPath = path
        pathStack.clear()
        breadcrumbs = listOf(Pair(remote.displayName ?: remoteName, path))
        fetchDirectory()
        saveGoToPreference(setAsDefault, startAtRoot = true)
    }

    fun goToHome(setAsDefault: Boolean) {
        startAtRoot = false
        directoryObject.clear()
        val path = "//$remoteName"
        directoryObject.setPath(path)
        currentPath = path
        pathStack.clear()
        breadcrumbs = listOf(Pair(remote.displayName ?: remoteName, path))
        fetchDirectory()
        saveGoToPreference(setAsDefault, startAtRoot = false)
    }

    fun invalidateCachePath(path: String) {
        if (directoryObject.isPathInCache(path)) {
            directoryObject.removePathFromCache(path)
        }
        if (directoryObject.currentPath == path) {
            fetchDirectory(silent = true)
        }
    }

    // --- Private helpers ---

    private fun restoreOrFetch(path: String) {
        if (!directoryObject.isContentValid(path) && directoryObject.isPathInCache(path)) {
            directoryObject.restoreFromCache(path)
            sortDirectoryContent()
            applyFilesToUI()
            currentPath = directoryObject.currentPath
            fetchDirectory(silent = true)
        } else if (directoryObject.isPathInCache(path)) {
            directoryObject.restoreFromCache(path)
            sortDirectoryContent()
            applyFilesToUI()
            currentPath = directoryObject.currentPath
        } else {
            directoryObject.setPath(path)
            currentPath = path
            files = emptyList()
            fetchDirectory()
        }
    }

    private fun sortDirectoryContent() {
        val content = directoryObject.directoryContent
        val comparator = when (sortOrder) {
            SortDialog.ALPHA_DESCENDING -> FileComparators.SortAlphaDescending()
            SortDialog.MOD_TIME_DESCENDING -> FileComparators.SortModTimeDescending()
            SortDialog.MOD_TIME_ASCENDING -> FileComparators.SortModTimeAscending()
            SortDialog.SIZE_DESCENDING -> FileComparators.SortSizeDescending()
            SortDialog.SIZE_ASCENDING -> FileComparators.SortSizeAscending()
            else -> FileComparators.SortAlphaAscending()
        }
        content.sortWith(comparator)
        directoryObject.setContent(content)
    }

    private fun applyFilesToUI() {
        val content = directoryObject.directoryContent
        files = if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            content.filter { it.name.lowercase().contains(query) }
        } else {
            content.toList()
        }
        currentPath = directoryObject.currentPath
    }

    private fun buildStackFromPath(path: String) {
        val root = "//$remoteName"
        pathStack.clear()
        if (root == path) return
        pathStack.addLast(root)

        var index = 0
        while (true) {
            index = path.indexOf("/", index)
            if (index <= 0) break
            pathStack.addLast(path.substring(0, index))
            index++
        }
    }

    private fun rebuildBreadcrumbs() {
        val root = "//$remoteName"
        val crumbs = mutableListOf(Pair(remote.displayName ?: remoteName, root))
        val path = directoryObject.currentPath
        if (path != root) {
            val suffix = path.removePrefix(root).removePrefix("/")
            val parts = suffix.split("/")
            var accumulated = root
            for (part in parts) {
                accumulated = "$accumulated/$part"
                crumbs.add(Pair(part, accumulated))
            }
        }
        breadcrumbs = crumbs
    }

    private fun saveGoToPreference(setAsDefault: Boolean, startAtRoot: Boolean) {
        val editor = prefs.edit()
        if (setAsDefault) {
            editor.putBoolean(context.getString(R.string.pref_key_go_to_default_set), true)
            editor.putBoolean(context.getString(R.string.pref_key_start_at_root), startAtRoot)
        } else {
            editor.putBoolean(context.getString(R.string.pref_key_go_to_default_set), false)
        }
        editor.apply()
    }

    companion object {
        private const val TAG = "FileExplorerViewModel"
        private const val SHARED_PREFS_SORT_ORDER = "ch.jacem.starclone.sort_order"
    }
}

data class TransferProgress(
    val id: String,
    val type: Type,
    val percent: Int,
    val content: String,
    val speed: String,
    val fileName: String,
)
