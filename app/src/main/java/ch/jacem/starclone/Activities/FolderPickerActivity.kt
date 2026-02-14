package ch.jacem.starclone.Activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.folderpicker.FolderPickerScreen
import ch.jacem.starclone.ui.screens.fileexplorer.FileExplorerViewModel
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper

class FolderPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REMOTE_NAME = "remote_name"
        const val EXTRA_INITIAL_PATH = "initial_path"
        const val RESULT_PATH = "selected_path"
    }

    private val viewModel by viewModels<FileExplorerViewModel>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(RuntimeConfiguration.attach(this, newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityHelper.applyTheme(this)
        setContentView(R.layout.activity_compose_shell)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.remote_folder_picker_title)

        val remoteName = intent.getStringExtra(EXTRA_REMOTE_NAME) ?: run {
            finish()
            return
        }

        val rclone = Rclone(this)
        val remoteItem = rclone.getRemoteItemFromName(remoteName) ?: run {
            finish()
            return
        }

        viewModel.initialize(remoteItem)

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                FolderPickerScreen(
                    viewModel = viewModel,
                    onSelectFolder = { path ->
                        setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_PATH, path))
                        finish()
                    },
                    onNavigateBack = { finish() },
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (viewModel.navigateUp()) return true
        finish()
        return true
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (!viewModel.navigateUp()) {
            super.onBackPressed()
        }
    }
}
