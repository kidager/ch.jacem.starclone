package ca.pkay.rcloneexplorer

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.preference.PreferenceManager
import ca.pkay.rcloneexplorer.Dialogs.SortDialog
import ca.pkay.rcloneexplorer.ui.screens.filepicker.FilePickerScreen
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme
import ca.pkay.rcloneexplorer.util.ActivityHelper
import ca.pkay.rcloneexplorer.util.FLog
import es.dmoral.toasty.Toasty
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class FilePicker : AppCompatActivity() {

    companion object {
        const val FILE_PICKER_PICK_DESTINATION_TYPE = "ca.pkay.rcexplorer.FILE_PICKER_PICK_DEST_TYPE"
        const val FILE_PICKER_RESULT = "ca.pkay.rcexplorer.FILE_PICKER_RESULT"
        private const val TAG = "FilePicker"
        private const val SHARED_PREFS_SORT_ORDER = "ca.pkay.rcexplorer.sort_order"
    }

    private var destinationPickerType = false
    private lateinit var root: File
    private lateinit var current: File
    private lateinit var availableStorage: ArrayList<String>
    private var sortOrder = SortDialog.ALPHA_ASCENDING

    // Compose state
    private var files by mutableStateOf<List<File>>(emptyList())
    private var currentPath by mutableStateOf("")
    private var rootPath by mutableStateOf("")
    private var selectedFiles by mutableStateOf<Set<String>>(emptySet())
    private var showFab by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.file_picker_root_title)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        sortOrder = prefs.getInt(SHARED_PREFS_SORT_ORDER, SortDialog.ALPHA_ASCENDING)

        destinationPickerType = intent.getBooleanExtra(FILE_PICKER_PICK_DESTINATION_TYPE, false)
        availableStorage = getStorageDirectories()
        root = File(availableStorage[0])
        current = root
        showFab = destinationPickerType

        refreshFileList()

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                FilePickerScreen(
                    files = files,
                    currentPath = currentPath,
                    rootPath = rootPath,
                    isDestinationMode = destinationPickerType,
                    selectedFiles = selectedFiles,
                    showFab = showFab,
                    onDirectoryClick = ::onDirectoryClicked,
                    onFileClick = ::onFileClicked,
                    onSelectionToggle = ::onSelectionToggle,
                    onBreadcrumbClick = ::onBreadcrumbClick,
                    onFabClick = ::onFabClicked,
                )
            }
        }
    }

    private fun refreshFileList() {
        val fileArray = current.listFiles()
        val list = fileArray?.toMutableList() ?: mutableListOf()
        sortFileList(list)
        files = list
        currentPath = current.absolutePath
        rootPath = root.absolutePath
    }

    private fun onDirectoryClicked(file: File) {
        current = file
        supportActionBar?.title = file.name
        refreshFileList()
        if (destinationPickerType) showFab = true
    }

    private fun onFileClicked(file: File) {
        val intent = Intent()
        intent.putExtra(FILE_PICKER_RESULT, arrayListOf(file))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun onSelectionToggle(file: File) {
        val path = file.absolutePath
        selectedFiles = if (selectedFiles.contains(path)) {
            selectedFiles - path
        } else {
            selectedFiles + path
        }
        showFab = destinationPickerType || selectedFiles.isNotEmpty()
    }

    private fun onBreadcrumbClick(path: String) {
        val target = File(path)
        if (target.exists() && target.isDirectory) {
            current = target
            supportActionBar?.title = if (current == root) {
                getString(R.string.file_picker_root_title)
            } else {
                current.name
            }
            refreshFileList()
        }
    }

    private fun onFabClicked() {
        if (destinationPickerType) {
            val intent = Intent()
            intent.putExtra(FILE_PICKER_RESULT, current.path)
            setResult(RESULT_OK, intent)
            finish()
        } else if (selectedFiles.isNotEmpty()) {
            val intent = Intent()
            intent.putExtra(FILE_PICKER_RESULT, ArrayList(selectedFiles.map { File(it) }))
            setResult(RESULT_OK, intent)
            finish()
        } else {
            Toasty.info(this, getString(R.string.nothing_selected), Toast.LENGTH_SHORT, true).show()
        }
    }

    // Menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.file_picker_menu, menu)
        if (destinationPickerType) {
            menu.removeItem(R.id.action_select_all)
        } else {
            menu.removeItem(R.id.action_new_folder)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_all -> {
                toggleSelectAll()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_new_folder -> {
                showNewFolderDialog()
                true
            }
            R.id.action_storage -> {
                showStorageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (current == root) {
            if (selectedFiles.isNotEmpty()) {
                val intent = Intent()
                intent.putExtra(FILE_PICKER_RESULT, ArrayList(selectedFiles.map { File(it) }))
                setResult(RESULT_OK, intent)
            }
            super.onBackPressed()
        } else {
            current = current.parentFile ?: root
            supportActionBar?.title = if (current == root) {
                getString(R.string.file_picker_root_title)
            } else {
                current.name
            }
            refreshFileList()
        }
    }

    // Dialogs

    private fun toggleSelectAll() {
        val allPaths = files.map { it.absolutePath }.toSet()
        selectedFiles = if (selectedFiles.containsAll(allPaths)) {
            emptySet()
        } else {
            selectedFiles + allPaths
        }
        showFab = destinationPickerType || selectedFiles.isNotEmpty()
    }

    private fun showSortDialog() {
        val asc = getString(R.string.sort_ascending)
        val desc = getString(R.string.sort_descending)
        val sortOptions = arrayOf(
            "${getString(R.string.sort_by_name)} ($asc)",
            "${getString(R.string.sort_by_name)} ($desc)",
            "${getString(R.string.sort_by_date_modified)} ($asc)",
            "${getString(R.string.sort_by_date_modified)} ($desc)",
            "${getString(R.string.sort_by_size)} ($asc)",
            "${getString(R.string.sort_by_size)} ($desc)",
        )
        val currentIndex = when (sortOrder) {
            SortDialog.ALPHA_ASCENDING -> 0
            SortDialog.ALPHA_DESCENDING -> 1
            SortDialog.MOD_TIME_ASCENDING -> 2
            SortDialog.MOD_TIME_DESCENDING -> 3
            SortDialog.SIZE_ASCENDING -> 4
            SortDialog.SIZE_DESCENDING -> 5
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.sort)
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, which ->
                sortOrder = when (which) {
                    0 -> SortDialog.ALPHA_ASCENDING
                    1 -> SortDialog.ALPHA_DESCENDING
                    2 -> SortDialog.MOD_TIME_ASCENDING
                    3 -> SortDialog.MOD_TIME_DESCENDING
                    4 -> SortDialog.SIZE_ASCENDING
                    5 -> SortDialog.SIZE_DESCENDING
                    else -> SortDialog.ALPHA_ASCENDING
                }
                PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putInt(SHARED_PREFS_SORT_ORDER, sortOrder).apply()
                refreshFileList()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showNewFolderDialog() {
        val editText = android.widget.EditText(this)
        editText.hint = getString(R.string.hint_new_folder)

        AlertDialog.Builder(this)
            .setTitle(R.string.create_new_folder)
            .setMessage(R.string.type_new_folder_name)
            .setView(editText)
            .setPositiveButton(R.string.okay_confirmation) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newDir = File(current.absolutePath, name)
                    if (newDir.mkdir()) {
                        refreshFileList()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showStorageDialog() {
        val selected = availableStorage.indexOf(root.absolutePath)
        val userSelected = intArrayOf(selected)

        AlertDialog.Builder(this)
            .setTitle(R.string.select_storage)
            .setSingleChoiceItems(
                availableStorage.toTypedArray(),
                selected,
            ) { _, which -> userSelected[0] = which }
            .setPositiveButton(R.string.select) { _, _ ->
                try {
                    switchStorage(userSelected[0])
                } catch (e: IOException) {
                    FLog.e(TAG, "Path not accessible", e)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun switchStorage(which: Int) {
        val newStorage = File(availableStorage[which])
        if (!newStorage.canRead()) throw IOException("Location not accessible")
        root = newStorage
        current = newStorage
        supportActionBar?.title = getString(R.string.file_picker_root_title)
        refreshFileList()
    }

    // Sorting

    private fun sortFileList(list: MutableList<File>) {
        val comparator: Comparator<File> = when (sortOrder) {
            SortDialog.ALPHA_ASCENDING -> FileComparators.SortFileAlphaAscending()
            SortDialog.ALPHA_DESCENDING -> FileComparators.SortFileAlphaDescending()
            SortDialog.MOD_TIME_ASCENDING -> FileComparators.SortFileModTimeAscending()
            SortDialog.MOD_TIME_DESCENDING -> FileComparators.SortFileModTimeDescending()
            SortDialog.SIZE_ASCENDING -> FileComparators.SortFileSizeAscending()
            SortDialog.SIZE_DESCENDING -> FileComparators.SortFileSizeDescending()
            else -> FileComparators.SortFileAlphaAscending()
        }
        list.sortWith(comparator)
    }

    // Storage discovery (from original FilePicker.java)

    @Suppress("DEPRECATION")
    private fun getStorageDirectories(): ArrayList<String> {
        val storageDirectories = ArrayList<String>()
        val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
        val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
        val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")

        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            if (TextUtils.isEmpty(rawExternalStorage)) {
                if (File("/storage/sdcard0").exists()) {
                    storageDirectories.add("/storage/sdcard0")
                } else {
                    storageDirectories.add(Environment.getExternalStorageDirectory().absolutePath)
                }
            } else {
                storageDirectories.add(canonicalizePath(rawExternalStorage!!))
            }
        } else {
            val path = Environment.getExternalStorageDirectory().absolutePath
            val folders = Pattern.compile("/").split(path)
            val lastFolder = folders.last()
            val isDigit = lastFolder.toIntOrNull() != null
            val rawUserId = if (isDigit) lastFolder else ""
            if (TextUtils.isEmpty(rawUserId)) {
                storageDirectories.add(rawEmulatedStorageTarget!!)
            } else {
                storageDirectories.add("$rawEmulatedStorageTarget/$rawUserId")
            }
        }

        if (destinationPickerType) {
            val internalStorage = storageDirectories[0]
            for (f in externalMediaDirs) {
                if (f != null && !f.absolutePath.startsWith(internalStorage)) {
                    storageDirectories.add(f.absolutePath)
                }
            }
        }

        if (!destinationPickerType && !TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            storageDirectories.addAll(rawSecondaryStoragesStr!!.split(File.pathSeparator))
        }

        val directories = getExternalFilesDirs(null)
        for (directory in directories) {
            try {
                val target = directory.parentFile?.parentFile?.parentFile?.parentFile
                if (target != null && target.canRead()) {
                    val targetPath = target.canonicalPath
                    if (!storageDirectories.contains(targetPath)) {
                        storageDirectories.add(targetPath)
                    }
                } else if (directory.canRead()) {
                    storageDirectories.add(directory.canonicalPath)
                }
            } catch (e: Exception) {
                FLog.i(TAG, "File discovery exception ", e)
            }
        }

        storageDirectories.removeAll { !File(it).canRead() }
        return storageDirectories
    }

    private fun canonicalizePath(path: String): String {
        return try {
            File(path).canonicalPath
        } catch (e: IOException) {
            path
        }
    }
}
