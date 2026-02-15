package ch.jacem.starclone.ui.screens.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.R
import ch.jacem.starclone.backup.BackupCategory
import ch.jacem.starclone.backup.BackupContents
import ch.jacem.starclone.backup.BackupUiState
import ch.jacem.starclone.backup.EncryptionScope
import ch.jacem.starclone.backup.ImportResult
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.PhoneAndroid

@Composable
fun BackupScreen(
    uiState: BackupUiState,
    onSetExportCategory: (BackupCategory, Boolean) -> Unit,
    onSetExportRemote: (String, Boolean) -> Unit,
    onProceedToEncryption: () -> Unit,
    onSetEncryptionScope: (EncryptionScope) -> Unit,
    onSetPassword: (String) -> Unit,
    onSetConfirmPassword: (String) -> Unit,
    onGoBackToContent: () -> Unit,
    onValidateAndExport: () -> Unit,
    onSetImportPassword: (String) -> Unit,
    onSubmitPassword: () -> Unit,
    onSetImportCategory: (BackupCategory, Boolean) -> Unit,
    onSetImportRemote: (String, Boolean) -> Unit,
    onStartImport: () -> Unit,
    onFinish: () -> Unit,
    onSelectImportFromFile: () -> Unit = {},
    onSelectImportFromRemote: (String) -> Unit = {},
    onDownloadRemoteFile: (String, String, String) -> Unit = { _, _, _ -> },
    remoteNames: List<String> = emptyList(),
) {
    when (uiState) {
        is BackupUiState.ExportSelectContent -> ExportSelectContentStep(
            state = uiState,
            onSetCategory = onSetExportCategory,
            onSetRemote = onSetExportRemote,
            onNext = onProceedToEncryption,
        )
        is BackupUiState.ExportEncryption -> ExportEncryptionStep(
            state = uiState,
            onSetScope = onSetEncryptionScope,
            onSetPassword = onSetPassword,
            onSetConfirmPassword = onSetConfirmPassword,
            onBack = onGoBackToContent,
            onExport = onValidateAndExport,
        )
        is BackupUiState.ExportInProgress -> ProgressStep(uiState.message)
        is BackupUiState.ExportComplete -> ExportCompleteStep(
            success = uiState.success,
            error = uiState.error,
            onFinish = onFinish,
        )
        is BackupUiState.ImportSourceSelection -> ImportSourceSelectionStep(
            hasRemotes = uiState.hasRemotes,
            remoteNames = remoteNames,
            onSelectFile = onSelectImportFromFile,
            onSelectRemote = onSelectImportFromRemote,
            onFinish = onFinish,
        )
        is BackupUiState.ImportWaitingForFile -> ProgressStep(
            stringResource(R.string.backup_waiting_for_file),
        )
        is BackupUiState.ImportRemoteFileSelection -> ImportRemoteFileSelectionStep(
            state = uiState,
            onSelectFile = { onDownloadRemoteFile(uiState.remoteName, uiState.backupPath, it) },
        )
        is BackupUiState.ImportPassword -> ImportPasswordStep(
            state = uiState,
            onSetPassword = onSetImportPassword,
            onSubmit = onSubmitPassword,
        )
        is BackupUiState.ImportPreview -> ImportPreviewStep(
            state = uiState,
            onSetCategory = onSetImportCategory,
            onSetRemote = onSetImportRemote,
            onImport = onStartImport,
        )
        is BackupUiState.ImportInProgress -> ProgressStep(uiState.message)
        is BackupUiState.ImportComplete -> ImportCompleteStep(
            result = uiState.result,
            error = uiState.error,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun ExportSelectContentStep(
    state: BackupUiState.ExportSelectContent,
    onSetCategory: (BackupCategory, Boolean) -> Unit,
    onSetRemote: (String, Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val anySelected = state.remotesEnabled || state.databaseEnabled || state.preferencesEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_select_content),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))

        CategoryCheckbox(
            label = stringResource(R.string.backup_category_remotes),
            description = "${state.selectedRemotes.size}/${state.availableRemotes.size} remotes",
            icon = Icons.Outlined.Cloud,
            checked = state.remotesEnabled,
            onToggle = { onSetCategory(BackupCategory.REMOTES, !state.remotesEnabled) },
        )

        // Per-remote checkboxes
        if (state.availableRemotes.isNotEmpty()) {
            for (remote in state.availableRemotes) {
                val isSelected = remote in state.selectedRemotes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetRemote(remote, !isSelected) }
                        .padding(start = 48.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = remote,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSetRemote(remote, it) },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        CategoryCheckbox(
            label = stringResource(R.string.backup_category_tasks),
            description = stringResource(R.string.backup_category_tasks_desc),
            icon = Icons.Outlined.Storage,
            checked = state.databaseEnabled,
            onToggle = { onSetCategory(BackupCategory.DATABASE, !state.databaseEnabled) },
        )
        CategoryCheckbox(
            label = stringResource(R.string.backup_category_preferences),
            description = stringResource(R.string.backup_category_preferences_desc),
            icon = Icons.Outlined.Settings,
            checked = state.preferencesEnabled,
            onToggle = { onSetCategory(BackupCategory.PREFERENCES, !state.preferencesEnabled) },
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = anySelected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.next))
        }
    }
}

@Composable
private fun CategoryCheckbox(
    label: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = supportingText,
    )
}

@Composable
private fun ExportEncryptionStep(
    state: BackupUiState.ExportEncryption,
    onSetScope: (EncryptionScope) -> Unit,
    onSetPassword: (String) -> Unit,
    onSetConfirmPassword: (String) -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_encryption_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.backup_encryption_recommended),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        EncryptionOption(
            label = stringResource(R.string.backup_encryption_everything),
            description = stringResource(R.string.backup_encryption_everything_desc),
            selected = state.scope == EncryptionScope.EVERYTHING,
            onClick = { onSetScope(EncryptionScope.EVERYTHING) },
        )
        EncryptionOption(
            label = stringResource(R.string.backup_encryption_config_only),
            description = stringResource(R.string.backup_encryption_config_only_desc),
            selected = state.scope == EncryptionScope.CONFIG_ONLY,
            onClick = { onSetScope(EncryptionScope.CONFIG_ONLY) },
        )
        EncryptionOption(
            label = stringResource(R.string.backup_encryption_none),
            description = stringResource(R.string.backup_encryption_none_desc),
            selected = state.scope == EncryptionScope.NONE,
            onClick = { onSetScope(EncryptionScope.NONE) },
        )

        if (state.scope != EncryptionScope.NONE) {
            Spacer(Modifier.height(16.dp))

            PasswordField(
                value = state.password,
                onValueChange = onSetPassword,
                label = stringResource(R.string.backup_password_label),
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                isError = state.passwordError != null,
            )
            Spacer(Modifier.height(8.dp))
            PasswordField(
                value = state.confirmPassword,
                onValueChange = onSetConfirmPassword,
                label = stringResource(R.string.backup_password_confirm_label),
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                isError = state.passwordError != null,
                supportingText = if (state.passwordError != null) {
                    { Text(state.passwordError) }
                } else null,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.back))
            }
            Button(
                onClick = onExport,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = " " + stringResource(R.string.export_rclone_config),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EncryptionOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportPasswordStep(
    state: BackupUiState.ImportPassword,
    onSetPassword: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.backup_enter_password),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        PasswordField(
            value = state.password,
            onValueChange = onSetPassword,
            label = stringResource(R.string.backup_password_label),
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
            isError = state.error != null,
            supportingText = if (state.error != null) {
                { Text(state.error) }
            } else null,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = state.password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_decrypt))
        }
    }
}

@Composable
private fun ImportPreviewStep(
    state: BackupUiState.ImportPreview,
    onSetCategory: (BackupCategory, Boolean) -> Unit,
    onSetRemote: (String, Boolean) -> Unit,
    onImport: () -> Unit,
) {
    val contents = state.contents
    val anySelected = state.remotesSelected || state.databaseSelected || state.preferencesSelected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_preview_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))

        // Remotes with per-remote selection
        if (contents.configBytes != null) {
            ImportCategoryCard(
                label = stringResource(R.string.backup_category_remotes),
                detail = "${state.selectedRemotes.size}/${contents.remoteNames.size} remotes",
                checked = state.remotesSelected,
                onToggle = { onSetCategory(BackupCategory.REMOTES, !state.remotesSelected) },
            )

            // Per-remote checkboxes
            for (remote in contents.remoteNames) {
                val isSelected = remote in state.selectedRemotes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetRemote(remote, !isSelected) }
                        .padding(start = 32.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = remote,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSetRemote(remote, it) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // Database (tasks, triggers, filters)
        if (contents.databaseJson != null) {
            ImportCategoryCard(
                label = stringResource(R.string.backup_category_tasks),
                detail = databaseSummary(contents),
                checked = state.databaseSelected,
                onToggle = { onSetCategory(BackupCategory.DATABASE, !state.databaseSelected) },
            )
        }

        // Preferences
        if (contents.hasPreferences) {
            ImportCategoryCard(
                label = stringResource(R.string.backup_category_preferences),
                detail = stringResource(R.string.backup_preferences_found),
                checked = state.preferencesSelected,
                onToggle = { onSetCategory(BackupCategory.PREFERENCES, !state.preferencesSelected) },
            )
        }

        if (state.contents.isLegacyFormat) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_legacy_format_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onImport,
            enabled = anySelected,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_import_button))
        }
    }
}

@Composable
private fun ImportCategoryCard(
    label: String,
    detail: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun databaseSummary(contents: BackupContents): String {
    val parts = mutableListOf<String>()
    if (contents.taskCount > 0) {
        parts.add(stringResource(R.string.backup_n_tasks, contents.taskCount))
    }
    if (contents.triggerCount > 0) {
        parts.add(stringResource(R.string.backup_n_triggers, contents.triggerCount))
    }
    if (contents.filterCount > 0) {
        parts.add(stringResource(R.string.backup_n_filters, contents.filterCount))
    }
    return if (parts.isEmpty()) {
        stringResource(R.string.backup_no_database_items)
    } else {
        parts.joinToString(", ")
    }
}

@Composable
private fun ProgressStep(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ExportCompleteStep(
    success: Boolean,
    error: String?,
    onFinish: () -> Unit,
) {
    CompleteStep(
        success = success,
        title = if (success) {
            stringResource(R.string.backup_export_success)
        } else {
            stringResource(R.string.backup_export_failed)
        },
        detail = error,
        onFinish = onFinish,
    )
}

@Composable
private fun ImportCompleteStep(
    result: ImportResult?,
    error: String?,
    onFinish: () -> Unit,
) {
    val detail = if (result != null) {
        buildImportSummary(result)
    } else {
        error
    }
    CompleteStep(
        success = result != null,
        title = if (result != null) {
            stringResource(R.string.backup_import_success)
        } else {
            stringResource(R.string.backup_import_failed)
        },
        detail = detail,
        onFinish = onFinish,
    )
}

@Composable
private fun buildImportSummary(result: ImportResult): String {
    val parts = mutableListOf<String>()
    if (result.remotesImported > 0) {
        parts.add(stringResource(R.string.backup_n_remotes_imported, result.remotesImported))
    }
    if (result.tasksImported > 0) {
        parts.add(stringResource(R.string.backup_n_tasks_imported, result.tasksImported))
    }
    if (result.triggersImported > 0) {
        parts.add(stringResource(R.string.backup_n_triggers_imported, result.triggersImported))
    }
    if (result.filtersImported > 0) {
        parts.add(stringResource(R.string.backup_n_filters_imported, result.filtersImported))
    }
    if (result.preferencesImported) {
        parts.add(stringResource(R.string.backup_preferences_imported))
    }
    return parts.joinToString("\n")
}

@Composable
private fun ImportSourceSelectionStep(
    hasRemotes: Boolean,
    remoteNames: List<String>,
    onSelectFile: () -> Unit,
    onSelectRemote: (String) -> Unit,
    onFinish: () -> Unit,
) {
    var showRemotePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.import_source_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))

        // From file
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFile() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.import_source_file), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.import_source_file_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // From Auto Backup
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.import_source_auto_backup), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.import_source_auto_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // From remote backup
        if (hasRemotes) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRemotePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.import_source_remote), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.import_source_remote_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(android.R.string.cancel))
        }
    }

    if (showRemotePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemotePicker = false },
            title = { Text(stringResource(R.string.import_source_remote_select)) },
            text = {
                Column {
                    remoteNames.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRemotePicker = false
                                    onSelectRemote(name)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(name, modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showRemotePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ImportRemoteFileSelectionStep(
    state: BackupUiState.ImportRemoteFileSelection,
    onSelectFile: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.import_source_remote_select_file),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${state.remoteName}:${state.backupPath}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.import_source_remote_loading))
                }
            }
        } else if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (state.files.isEmpty()) {
            Text(
                text = stringResource(R.string.import_source_remote_no_backups),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.files, key = { it }, contentType = { "backup" }) { fileName ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFile(fileName) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = fileName,
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteStep(
    success: Boolean,
    title: String,
    detail: String?,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (success) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        if (detail != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onFinish) {
            Text(stringResource(R.string.done))
        }
    }
}
