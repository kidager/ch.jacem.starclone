package ch.jacem.starclone.Activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import ch.jacem.starclone.R
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.ui.screens.tasks.edit.TaskEditScreen
import ch.jacem.starclone.ui.screens.tasks.edit.TaskEditViewModel
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper
import ch.jacem.starclone.util.UriUtils

class TaskActivity : AppCompatActivity() {

    companion object {
        const val ID_EXTRA = "TASK_EDIT_ID"
    }

    private val viewModel by viewModels<TaskEditViewModel>()

    private val localPathPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val path = UriUtils.getPathFromTreeUri(this, uri)
            if (path != null) {
                viewModel.updateLocalPath(path)
            }
        }
    }

    private val remotePathPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(FolderPickerActivity.RESULT_PATH) ?: ""
            viewModel.updateRemotePath(path)
        }
    }

    private val remotePath2Picker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(FolderPickerActivity.RESULT_PATH) ?: ""
            viewModel.updateRemotePath2(path)
        }
    }

    private val filterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val filterId = result.data?.getLongExtra(FilterActivity.SAVED_FILTER_ID_EXTRA, -1) ?: -1
            if (filterId >= 0) {
                viewModel.onFilterCreated(filterId)
            }
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val taskId = intent.getLongExtra(ID_EXTRA, 0)
        supportActionBar?.title = getString(
            if (taskId != 0L) R.string.task_edit_title_edit else R.string.task_edit_title_new,
        )

        if (savedInstanceState == null) {
            viewModel.loadTask(taskId)
        }

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                TaskEditScreen(
                    viewModel = viewModel,
                    onPickLocalPath = { localPathPicker.launch(null) },
                    onPickRemotePath = { remoteName ->
                        val intent = Intent(this, FolderPickerActivity::class.java).apply {
                            putExtra(FolderPickerActivity.EXTRA_REMOTE_NAME, remoteName)
                        }
                        remotePathPicker.launch(intent)
                    },
                    onPickRemotePath2 = { remoteName ->
                        val intent = Intent(this, FolderPickerActivity::class.java).apply {
                            putExtra(FolderPickerActivity.EXTRA_REMOTE_NAME, remoteName)
                        }
                        remotePath2Picker.launch(intent)
                    },
                    onCreateFilter = {
                        filterLauncher.launch(Intent(this, FilterActivity::class.java))
                    },
                    onEditFilter = { filterId ->
                        val intent = Intent(this, FilterActivity::class.java).apply {
                            putExtra(FilterActivity.ID_EXTRA, filterId)
                        }
                        filterLauncher.launch(intent)
                    },
                    onSave = {
                        if (viewModel.save()) {
                            finish()
                        }
                    },
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
