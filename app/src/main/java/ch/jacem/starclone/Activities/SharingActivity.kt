package ch.jacem.starclone.Activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import ch.jacem.starclone.Items.RemoteItem
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.sharing.ShareRemotesScreen
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper
import ch.jacem.starclone.util.FLog
import ch.jacem.starclone.workmanager.EphemeralTaskManager
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.HashSet

class SharingActivity : AppCompatActivity() {

    private val uploadList = ArrayList<String>()
    @Volatile
    private var isDataReady = false
    private var remotes: List<RemoteItem> = emptyList()
    private var selectedRemote: RemoteItem? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val remote = selectedRemote ?: return@registerForActivityResult
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(FolderPickerActivity.RESULT_PATH) ?: "/"
            onShareDestinationSelected(remote, path)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.remotes_toolbar_title)

        val rclone = Rclone(this)
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            copyFiles(listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM)))
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val uris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            if (uris != null) copyFiles(uris) else { finish(); return }
        } else {
            finish()
            return
        }

        if (rclone.isConfigEncrypted || !rclone.isConfigFileCreated || rclone.remotes.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_not_configured)
                .setMessage(R.string.open_app_to_configure)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .show()
        } else {
            loadRemotes(rclone)
            showRemotesList()
        }
    }

    private fun loadRemotes(rclone: Rclone) {
        val allRemotes = rclone.remotes
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val hiddenIds = prefs.getStringSet(getString(R.string.shared_preferences_hidden_remotes), HashSet()) ?: HashSet()
        remotes = allRemotes.filter { !hiddenIds.contains(it.name) }
        RemoteItem.prepareDisplay(this, remotes)
        Collections.sort(remotes)
    }

    private fun showRemotesList() {
        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                ShareRemotesScreen(
                    remotes = remotes,
                    onRemoteClick = ::onRemoteClick,
                )
            }
        }
    }

    private fun onRemoteClick(remote: RemoteItem) {
        selectedRemote = remote
        val intent = Intent(this, FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_REMOTE_NAME, remote.name)
        }
        folderPickerLauncher.launch(intent)
    }

    private fun onShareDestinationSelected(remote: RemoteItem, path: String) {
        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            while (!isDataReady) {
                kotlinx.coroutines.delay(500)
            }
            withContext(Dispatchers.Main) {
                for (uploadFile in uploadList) {
                    EphemeralTaskManager.queueUpload(context, remote, uploadFile, path)
                }
                finish()
            }
        }
    }

    @SuppressLint("Recycle")
    private fun copyFiles(uris: List<Uri>) {
        isDataReady = false
        CoroutineScope(Dispatchers.IO).launch {
            var success = true
            for (uri in uris) {
                if (uri.scheme == null || uri.path == null) {
                    FLog.w(TAG, "Can't copy invalid uri %s", uri)
                    success = false
                    continue
                }
                val fileName = try {
                    resolveName(uri)
                } catch (e: SecurityException) {
                    success = false
                    continue
                }
                val cacheDir = externalCacheDir ?: continue
                val outFile = File(cacheDir, fileName)
                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                            uploadList.add(outFile.absolutePath)
                        }
                    } ?: run { success = false }
                } catch (e: IOException) {
                    FLog.e(TAG, "Copy error: ", e)
                    success = false
                }
            }
            if (!success) {
                withContext(Dispatchers.Main) {
                    Toasty.error(this@SharingActivity, getString(R.string.error_retrieving_files), Toast.LENGTH_LONG, true).show()
                    finish()
                }
                return@launch
            }
            isDataReady = true
        }
    }

    private fun resolveName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.let { return it }
                }
            }
        }
        val segments = uri.pathSegments
        if (segments.isNotEmpty()) return segments.last()
        return "unnamed"
    }

    companion object {
        private const val TAG = "SharingActivity"
    }
}
