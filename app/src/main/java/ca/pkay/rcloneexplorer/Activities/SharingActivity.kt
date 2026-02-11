package ca.pkay.rcloneexplorer.Activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import ca.pkay.rcloneexplorer.Fragments.ShareFragment
import ca.pkay.rcloneexplorer.Fragments.ShareRemotesFragment
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.RuntimeConfiguration
import ca.pkay.rcloneexplorer.ui.screens.sharing.ShareRemotesScreen
import ca.pkay.rcloneexplorer.ui.theme.StarCloneTheme
import ca.pkay.rcloneexplorer.util.ActivityHelper
import ca.pkay.rcloneexplorer.util.FLog
import ca.pkay.rcloneexplorer.workmanager.EphemeralTaskManager
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

class SharingActivity : AppCompatActivity(),
    ShareFragment.OnShareDestinationSelected {

    private var fragment: androidx.fragment.app.Fragment? = null
    private val uploadList = ArrayList<String>()
    @Volatile
    private var isDataReady = false
    private var remotes: List<RemoteItem> = emptyList()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_sharing)
        setSupportActionBar(findViewById(R.id.toolbar))

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
        // Hide the fragment container and show Compose remotes list
        val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.flFragment)
        fragmentContainer.visibility = android.view.View.GONE

        // Add ComposeView dynamically
        val coordinatorLayout = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.cl_activity_settings)
        val composeView = ComposeView(this).apply {
            id = android.view.View.generateViewId()
            layoutParams = androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                behavior = com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior()
            }
        }
        coordinatorLayout.addView(composeView)

        title = getString(R.string.remotes_toolbar_title)
        composeView.setContent {
            StarCloneTheme {
                ShareRemotesScreen(
                    remotes = remotes,
                    onRemoteClick = ::onRemoteClick,
                )
            }
        }
    }

    private fun onRemoteClick(remote: RemoteItem) {
        // Remove the dynamically added ComposeView
        val coordinatorLayout = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.cl_activity_settings)
        // Remove any ComposeView children (the one we dynamically added)
        for (i in coordinatorLayout.childCount - 1 downTo 0) {
            val child = coordinatorLayout.getChildAt(i)
            if (child is ComposeView) {
                coordinatorLayout.removeView(child)
            }
        }

        // Show fragment container and start ShareFragment
        val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.flFragment)
        fragmentContainer.visibility = android.view.View.VISIBLE

        fragment = ShareFragment.newInstance(remote)
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment!!)
            .addToBackStack(null)
            .commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val f = fragment
        if (f is ShareFragment && f.onBackButtonPressed()) {
            return
        }
        super.onBackPressed()
    }

    override fun onShareDestinationSelected(remote: RemoteItem, path: String) {
        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            // Wait for copy to finish
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
