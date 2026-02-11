package ch.jacem.starclone.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.jacem.starclone.util.FLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class BackupMode { EXPORT, IMPORT }

sealed interface BackupUiState {

    data class ExportSelectContent(
        val remotesEnabled: Boolean = true,
        val databaseEnabled: Boolean = true,
        val preferencesEnabled: Boolean = true,
        val availableRemotes: List<String> = emptyList(),
        val selectedRemotes: Set<String> = emptySet(),
    ) : BackupUiState

    data class ExportEncryption(
        val categories: Set<BackupCategory> = emptySet(),
        val selectedRemotes: Set<String> = emptySet(),
        val scope: EncryptionScope = EncryptionScope.EVERYTHING,
        val password: String = "",
        val confirmPassword: String = "",
        val passwordError: String? = null,
    ) : BackupUiState

    data class ExportInProgress(val message: String) : BackupUiState

    data class ExportComplete(
        val success: Boolean,
        val error: String? = null,
    ) : BackupUiState

    data object ImportWaitingForFile : BackupUiState

    data class ImportPassword(
        val sourceUri: Uri,
        val password: String = "",
        val error: String? = null,
    ) : BackupUiState

    data class ImportPreview(
        val sourceUri: Uri,
        val contents: BackupContents,
        val remotesSelected: Boolean = true,
        val databaseSelected: Boolean = true,
        val preferencesSelected: Boolean = true,
        val selectedRemotes: Set<String> = emptySet(),
    ) : BackupUiState

    data class ImportInProgress(val message: String) : BackupUiState

    data class ImportComplete(
        val result: ImportResult? = null,
        val error: String? = null,
    ) : BackupUiState
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.ExportSelectContent())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val context get() = getApplication<Application>()

    fun initMode(mode: BackupMode) {
        _uiState.value = when (mode) {
            BackupMode.EXPORT -> {
                val remotes = loadCurrentRemotes()
                BackupUiState.ExportSelectContent(
                    availableRemotes = remotes,
                    selectedRemotes = remotes.toSet(),
                )
            }
            BackupMode.IMPORT -> BackupUiState.ImportWaitingForFile
        }
    }

    private fun loadCurrentRemotes(): List<String> {
        val configFile = File(context.filesDir, "rclone.conf")
        if (!configFile.exists()) return emptyList()
        return BackupReader.parseRemoteNames(configFile.readText())
    }

    // --- Export ---

    fun setExportCategory(category: BackupCategory, enabled: Boolean) {
        val state = _uiState.value as? BackupUiState.ExportSelectContent ?: return
        _uiState.value = when (category) {
            BackupCategory.REMOTES -> state.copy(
                remotesEnabled = enabled,
                selectedRemotes = if (enabled) state.availableRemotes.toSet() else emptySet(),
            )
            BackupCategory.DATABASE -> state.copy(databaseEnabled = enabled)
            BackupCategory.PREFERENCES -> state.copy(preferencesEnabled = enabled)
        }
    }

    fun setExportRemote(name: String, selected: Boolean) {
        val state = _uiState.value as? BackupUiState.ExportSelectContent ?: return
        val newSelected = if (selected) state.selectedRemotes + name else state.selectedRemotes - name
        _uiState.value = state.copy(
            selectedRemotes = newSelected,
            remotesEnabled = newSelected.isNotEmpty(),
        )
    }

    fun proceedToEncryption() {
        val state = _uiState.value as? BackupUiState.ExportSelectContent ?: return
        val categories = buildSet {
            if (state.remotesEnabled && state.selectedRemotes.isNotEmpty()) add(BackupCategory.REMOTES)
            if (state.databaseEnabled) add(BackupCategory.DATABASE)
            if (state.preferencesEnabled) add(BackupCategory.PREFERENCES)
        }
        _uiState.value = BackupUiState.ExportEncryption(
            categories = categories,
            selectedRemotes = state.selectedRemotes,
        )
    }

    fun setEncryptionScope(scope: EncryptionScope) {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return
        _uiState.value = state.copy(scope = scope, passwordError = null)
    }

    fun setPassword(password: String) {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return
        _uiState.value = state.copy(password = password, passwordError = null)
    }

    fun setConfirmPassword(password: String) {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return
        _uiState.value = state.copy(confirmPassword = password, passwordError = null)
    }

    fun goBackToContentSelection() {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return
        val remotes = loadCurrentRemotes()
        _uiState.value = BackupUiState.ExportSelectContent(
            remotesEnabled = BackupCategory.REMOTES in state.categories,
            databaseEnabled = BackupCategory.DATABASE in state.categories,
            preferencesEnabled = BackupCategory.PREFERENCES in state.categories,
            availableRemotes = remotes,
            selectedRemotes = state.selectedRemotes,
        )
    }

    fun validateExportPassword(): Boolean {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return false
        if (state.scope != EncryptionScope.NONE) {
            if (state.password.length < 8) {
                _uiState.value = state.copy(passwordError = "Password must be at least 8 characters")
                return false
            }
            if (state.password != state.confirmPassword) {
                _uiState.value = state.copy(passwordError = "Passwords don't match")
                return false
            }
        }
        return true
    }

    fun startExport(targetUri: Uri) {
        val state = _uiState.value as? BackupUiState.ExportEncryption ?: return
        _uiState.value = BackupUiState.ExportInProgress("Exporting backup...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                BackupWriter.writeBackup(
                    context = context,
                    targetUri = targetUri,
                    categories = state.categories,
                    encryptionScope = state.scope,
                    password = if (state.scope != EncryptionScope.NONE) state.password else null,
                    selectedRemotes = state.selectedRemotes.takeIf { it.isNotEmpty() },
                )
                _uiState.value = BackupUiState.ExportComplete(success = true)
            } catch (e: Exception) {
                FLog.e(TAG, "Export failed", e)
                _uiState.value = BackupUiState.ExportComplete(
                    success = false,
                    error = e.message ?: "Export failed",
                )
            }
        }
    }

    // --- Import ---

    fun onFileSelected(uri: Uri) {
        _uiState.value = BackupUiState.ImportInProgress("Reading backup file...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contents = BackupReader.readBackup(context, uri, password = null)
                if (contents.isEncrypted) {
                    _uiState.value = BackupUiState.ImportPassword(sourceUri = uri)
                } else {
                    _uiState.value = BackupUiState.ImportPreview(
                        sourceUri = uri,
                        contents = contents,
                        remotesSelected = contents.configBytes != null,
                        databaseSelected = contents.databaseJson != null,
                        preferencesSelected = contents.hasPreferences,
                        selectedRemotes = contents.remoteNames.toSet(),
                    )
                }
            } catch (e: Exception) {
                FLog.e(TAG, "Failed to read backup", e)
                _uiState.value = BackupUiState.ImportComplete(
                    error = e.message ?: "Failed to read backup file",
                )
            }
        }
    }

    fun setImportPassword(password: String) {
        val state = _uiState.value as? BackupUiState.ImportPassword ?: return
        _uiState.value = state.copy(password = password, error = null)
    }

    fun submitPassword() {
        val state = _uiState.value as? BackupUiState.ImportPassword ?: return
        _uiState.value = BackupUiState.ImportInProgress("Decrypting backup...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contents = BackupReader.readBackup(context, state.sourceUri, state.password)
                _uiState.value = BackupUiState.ImportPreview(
                    sourceUri = state.sourceUri,
                    contents = contents,
                    remotesSelected = contents.configBytes != null,
                    databaseSelected = contents.databaseJson != null,
                    preferencesSelected = contents.hasPreferences,
                    selectedRemotes = contents.remoteNames.toSet(),
                )
            } catch (e: BadPasswordException) {
                _uiState.value = state.copy(error = "Incorrect password")
            } catch (e: Exception) {
                FLog.e(TAG, "Failed to decrypt backup", e)
                _uiState.value = BackupUiState.ImportComplete(
                    error = e.message ?: "Failed to decrypt backup",
                )
            }
        }
    }

    fun setImportCategory(category: BackupCategory, selected: Boolean) {
        val state = _uiState.value as? BackupUiState.ImportPreview ?: return
        _uiState.value = when (category) {
            BackupCategory.REMOTES -> state.copy(
                remotesSelected = selected,
                selectedRemotes = if (selected) state.contents.remoteNames.toSet() else emptySet(),
            )
            BackupCategory.DATABASE -> state.copy(databaseSelected = selected)
            BackupCategory.PREFERENCES -> state.copy(preferencesSelected = selected)
        }
    }

    fun setImportRemote(name: String, selected: Boolean) {
        val state = _uiState.value as? BackupUiState.ImportPreview ?: return
        val newSelected = if (selected) state.selectedRemotes + name else state.selectedRemotes - name
        _uiState.value = state.copy(
            selectedRemotes = newSelected,
            remotesSelected = newSelected.isNotEmpty(),
        )
    }

    fun startImport() {
        val state = _uiState.value as? BackupUiState.ImportPreview ?: return
        _uiState.value = BackupUiState.ImportInProgress("Importing backup...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categories = buildSet {
                    if (state.remotesSelected) add(BackupCategory.REMOTES)
                    if (state.databaseSelected) add(BackupCategory.DATABASE)
                    if (state.preferencesSelected) add(BackupCategory.PREFERENCES)
                }

                val result = BackupImporter.importBackup(
                    context = context,
                    contents = state.contents,
                    categories = categories,
                    selectedRemotes = state.selectedRemotes.takeIf { it.isNotEmpty() },
                )
                _uiState.value = BackupUiState.ImportComplete(result = result)
            } catch (e: Exception) {
                FLog.e(TAG, "Import failed", e)
                _uiState.value = BackupUiState.ImportComplete(
                    error = e.message ?: "Import failed",
                )
            }
        }
    }

    companion object {
        private const val TAG = "BackupViewModel"
    }
}
