package ch.jacem.starclone.Activities

import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ch.jacem.starclone.R
import ch.jacem.starclone.RuntimeConfiguration
import ch.jacem.starclone.backup.BackupMode
import ch.jacem.starclone.backup.BackupUiState
import ch.jacem.starclone.backup.BackupViewModel
import ch.jacem.starclone.ui.screens.backup.BackupScreen
import ch.jacem.starclone.ui.theme.StarCloneTheme
import ch.jacem.starclone.util.ActivityHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BackupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "backup_mode"
    }

    private val viewModel by viewModels<BackupViewModel>()

    private val exportFilePicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            viewModel.startExport(uri)
        }
    }

    private val importFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onFileSelected(uri)
        } else {
            finish()
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

        val mode = intent.getStringExtra(EXTRA_MODE) ?: "export"
        val backupMode = if (mode == "import") BackupMode.IMPORT else BackupMode.EXPORT

        supportActionBar?.title = getString(
            if (backupMode == BackupMode.EXPORT) R.string.export_rclone_config
            else R.string.import_rclone_config,
        )

        if (savedInstanceState == null) {
            viewModel.initMode(backupMode)
        }

        // Launch file picker immediately for import
        if (savedInstanceState == null && backupMode == BackupMode.IMPORT) {
            importFilePicker.launch(arrayOf("*/*"))
        }

        // Update toolbar title based on state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    supportActionBar?.title = when (state) {
                        is BackupUiState.ExportSelectContent,
                        is BackupUiState.ExportEncryption,
                        is BackupUiState.ExportInProgress,
                        is BackupUiState.ExportComplete -> getString(R.string.export_rclone_config)
                        else -> getString(R.string.import_rclone_config)
                    }
                }
            }
        }

        findViewById<ComposeView>(R.id.compose_view).setContent {
            StarCloneTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                BackupScreen(
                    uiState = uiState.value,
                    onSetExportCategory = viewModel::setExportCategory,
                    onSetExportRemote = viewModel::setExportRemote,
                    onProceedToEncryption = viewModel::proceedToEncryption,
                    onSetEncryptionScope = viewModel::setEncryptionScope,
                    onSetPassword = viewModel::setPassword,
                    onSetConfirmPassword = viewModel::setConfirmPassword,
                    onGoBackToContent = viewModel::goBackToContentSelection,
                    onValidateAndExport = ::onValidateAndExport,
                    onSetImportPassword = viewModel::setImportPassword,
                    onSubmitPassword = viewModel::submitPassword,
                    onSetImportCategory = viewModel::setImportCategory,
                    onSetImportRemote = viewModel::setImportRemote,
                    onStartImport = viewModel::startImport,
                    onFinish = ::finish,
                )
            }
        }
    }

    private fun onValidateAndExport() {
        if (viewModel.validateExportPassword()) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
                .format(Calendar.getInstance().time)
            exportFilePicker.launch("${getString(R.string.app_name)}-backup-$timestamp.zip")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
