package ch.jacem.starclone.ui.screens.settings.cloudbackup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.backup.AgeEncryption
import ch.jacem.starclone.backup.CloudBackupScheduler
import ch.jacem.starclone.backup.KeystoreHelper
import ch.jacem.starclone.ui.components.StarCloneDialog
import ch.jacem.starclone.ui.components.StarCloneListItem
import ch.jacem.starclone.ui.components.StarCloneTopBar
import java.text.DateFormat
import java.util.Date

private const val DEST_AUTO_BACKUP = "auto_backup"
private const val DEST_CUSTOM_REMOTE = "custom_remote"
private const val ENCRYPTION_NONE = "none"
private const val ENCRYPTION_PASSWORD = "password"
private const val ENCRYPTION_AGE = "age"
private const val SECRET_KEY_PASSWORD = "cloud_backup_password"
private const val SECRET_KEY_AGE_SECRET = "cloud_backup_age_secret"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudBackupScreen(onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val rclone = remember { Rclone(context) }

    // Pref keys
    val enabledKey = stringResource(R.string.pref_key_cloud_backup_enabled)
    val destinationKey = stringResource(R.string.pref_key_cloud_backup_destination)
    val remoteKey = stringResource(R.string.pref_key_cloud_backup_remote)
    val pathKey = stringResource(R.string.pref_key_cloud_backup_path)
    val frequencyKey = stringResource(R.string.pref_key_cloud_backup_frequency)
    val timeKey = stringResource(R.string.pref_key_cloud_backup_time)
    val wifiOnlyKey = stringResource(R.string.pref_key_cloud_backup_wifi_only)
    val retentionKey = stringResource(R.string.pref_key_cloud_backup_retention)
    val encryptionKey = stringResource(R.string.pref_key_cloud_backup_encryption)
    val lastTimeKey = stringResource(R.string.pref_key_cloud_backup_last_time)
    val lastStatusKey = stringResource(R.string.pref_key_cloud_backup_last_status)
    val agePublicKeyKey = stringResource(R.string.pref_key_cloud_backup_age_public_key)

    // State
    var enabled by remember { mutableStateOf(prefs.getBoolean(enabledKey, false)) }
    var destination by remember { mutableStateOf(prefs.getString(destinationKey, DEST_CUSTOM_REMOTE) ?: DEST_CUSTOM_REMOTE) }
    var selectedRemote by remember { mutableStateOf(prefs.getString(remoteKey, "") ?: "") }
    var backupPath by remember { mutableStateOf(prefs.getString(pathKey, "StarClone-Backups") ?: "StarClone-Backups") }
    var frequency by remember { mutableIntStateOf(prefs.getInt(frequencyKey, CloudBackupScheduler.FREQUENCY_DAILY)) }
    var timeMinutes by remember { mutableIntStateOf(prefs.getInt(timeKey, 3 * 60)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean(wifiOnlyKey, true)) }
    var retention by remember { mutableIntStateOf(prefs.getInt(retentionKey, 5)) }
    var encryption by remember { mutableStateOf(prefs.getString(encryptionKey, ENCRYPTION_NONE) ?: ENCRYPTION_NONE) }
    var agePublicKey by remember { mutableStateOf(prefs.getString(agePublicKeyKey, "") ?: "") }
    val hasPassword = remember { mutableStateOf(KeystoreHelper.hasSecret(context, SECRET_KEY_PASSWORD)) }
    val hasAgeKey = remember { mutableStateOf(KeystoreHelper.hasSecret(context, SECRET_KEY_AGE_SECRET)) }

    // Remotes
    val remotes = remember { rclone.remotes?.map { it.name } ?: emptyList() }

    // Last backup info
    val lastBackupTime = remember { prefs.getLong(lastTimeKey, 0L) }
    val lastBackupSuccess = remember { prefs.getBoolean(lastStatusKey, false) }

    // Dialog state
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEncryptionDialog by remember { mutableStateOf(false) }
    var showAgeKeyDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    var showKeyLossWarning by remember { mutableStateOf(false) }
    var showKeyChangeWarning by remember { mutableStateOf(false) }

    // Pending action after warning confirmation
    var pendingPasswordSave by remember { mutableStateOf<String?>(null) }
    var pendingAgeSave by remember { mutableStateOf<Pair<String, String>?>(null) } // publicKey, secretKey

    // Imported age key file content
    var importedAgePublicKey by remember { mutableStateOf("") }
    var importedAgeSecretKey by remember { mutableStateOf("") }

    val ageKeyFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
            var pubKey = ""
            var secKey = ""
            for (line in lines) {
                if (line.startsWith("AGE-SECRET-KEY-")) {
                    secKey = line
                } else if (line.startsWith("age1")) {
                    pubKey = line
                }
            }
            importedAgePublicKey = pubKey
            importedAgeSecretKey = secKey
            showAgeKeyDialog = true
        }
    }

    fun reschedule() {
        if (enabled && destination == DEST_CUSTOM_REMOTE) {
            CloudBackupScheduler.schedule(context)
        } else {
            CloudBackupScheduler.cancel(context)
        }
    }

    fun isUpdatingExistingKey(): Boolean {
        return when (encryption) {
            ENCRYPTION_PASSWORD -> hasPassword.value
            ENCRYPTION_AGE -> hasAgeKey.value
            else -> false
        }
    }

    fun commitPasswordSave(password: String) {
        KeystoreHelper.storeSecret(context, SECRET_KEY_PASSWORD, password)
        hasPassword.value = true
    }

    fun commitAgeSave(publicKey: String, secretKey: String) {
        agePublicKey = publicKey
        prefs.edit().putString(agePublicKeyKey, publicKey).apply()
        if (secretKey.isNotBlank()) {
            KeystoreHelper.storeSecret(context, SECRET_KEY_AGE_SECRET, secretKey)
            hasAgeKey.value = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StarCloneTopBar(
            title = stringResource(R.string.cloud_backup_title),
            navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            onNavigationClick = onNavigateBack,
        )

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            // Enable toggle
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.cloud_backup_enabled),
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                prefs.edit().putBoolean(enabledKey, it).apply()
                                reschedule()
                            },
                        )
                    },
                    onClick = {
                        enabled = !enabled
                        prefs.edit().putBoolean(enabledKey, enabled).apply()
                        reschedule()
                    },
                )
            }

            if (enabled) {
                // Destination section
                item { SectionHeader(stringResource(R.string.cloud_backup_destination)) }

                item {
                    RadioItem(
                        label = stringResource(R.string.cloud_backup_auto_backup),
                        description = stringResource(R.string.cloud_backup_auto_backup_desc),
                        selected = destination == DEST_AUTO_BACKUP,
                        onClick = {
                            destination = DEST_AUTO_BACKUP
                            prefs.edit().putString(destinationKey, DEST_AUTO_BACKUP).apply()
                            reschedule()
                        },
                    )
                }
                item {
                    RadioItem(
                        label = stringResource(R.string.cloud_backup_custom_remote),
                        description = stringResource(R.string.cloud_backup_custom_remote_desc),
                        selected = destination == DEST_CUSTOM_REMOTE,
                        onClick = {
                            destination = DEST_CUSTOM_REMOTE
                            prefs.edit().putString(destinationKey, DEST_CUSTOM_REMOTE).apply()
                            reschedule()
                        },
                    )
                }

                // Custom remote settings (only shown for custom remote)
                if (destination == DEST_CUSTOM_REMOTE) {
                    item { SectionHeader(stringResource(R.string.cloud_backup_remote)) }

                    item {
                        if (remotes.isEmpty()) {
                            StarCloneListItem(headlineText = stringResource(R.string.cloud_backup_no_remotes))
                        } else {
                            StarCloneListItem(
                                headlineText = stringResource(R.string.cloud_backup_select_remote),
                                supportingText = selectedRemote.ifBlank { null },
                                onClick = { showRemoteDialog = true },
                            )
                        }
                    }

                    item {
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_path),
                            supportingText = backupPath,
                            onClick = { showPathDialog = true },
                        )
                    }

                    // Schedule section
                    item { SectionHeader(stringResource(R.string.cloud_backup_frequency)) }

                    item {
                        val frequencyLabel = when (frequency) {
                            CloudBackupScheduler.FREQUENCY_DAILY -> stringResource(R.string.cloud_backup_frequency_daily)
                            CloudBackupScheduler.FREQUENCY_WEEKLY -> stringResource(R.string.cloud_backup_frequency_weekly)
                            CloudBackupScheduler.FREQUENCY_MONTHLY -> stringResource(R.string.cloud_backup_frequency_monthly)
                            else -> stringResource(R.string.cloud_backup_frequency_daily)
                        }
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_frequency),
                            supportingText = frequencyLabel,
                            onClick = { showFrequencyDialog = true },
                        )
                    }

                    item {
                        val hours = timeMinutes / 60
                        val minutes = timeMinutes % 60
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_time),
                            supportingText = String.format("%02d:%02d", hours, minutes),
                            onClick = { showTimeDialog = true },
                        )
                    }

                    item {
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_wifi_only),
                            supportingText = stringResource(R.string.cloud_backup_wifi_only_desc),
                            trailingContent = {
                                Switch(
                                    checked = wifiOnly,
                                    onCheckedChange = {
                                        wifiOnly = it
                                        prefs.edit().putBoolean(wifiOnlyKey, it).apply()
                                        reschedule()
                                    },
                                )
                            },
                            onClick = {
                                wifiOnly = !wifiOnly
                                prefs.edit().putBoolean(wifiOnlyKey, wifiOnly).apply()
                                reschedule()
                            },
                        )
                    }

                    item {
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_retention),
                            supportingText = retention.toString(),
                            onClick = { showRetentionDialog = true },
                        )
                    }
                }

                // Encryption section — shared for both destinations
                item { SectionHeader(stringResource(R.string.cloud_backup_encryption)) }

                item {
                    val encLabel = when (encryption) {
                        ENCRYPTION_NONE -> stringResource(R.string.cloud_backup_encryption_none)
                        ENCRYPTION_PASSWORD -> stringResource(R.string.cloud_backup_encryption_password)
                        ENCRYPTION_AGE -> stringResource(R.string.cloud_backup_encryption_age)
                        else -> stringResource(R.string.cloud_backup_encryption_none)
                    }
                    StarCloneListItem(
                        headlineText = stringResource(R.string.cloud_backup_encryption),
                        supportingText = encLabel,
                        onClick = { showEncryptionDialog = true },
                    )
                }

                // Password setup (if password encryption selected)
                if (encryption == ENCRYPTION_PASSWORD) {
                    item {
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_set_password),
                            supportingText = if (hasPassword.value) "****" else null,
                            onClick = { showPasswordDialog = true },
                        )
                    }
                }

                // Age key setup (if age encryption selected)
                if (encryption == ENCRYPTION_AGE) {
                    item {
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_age_public_key),
                            supportingText = agePublicKey.ifBlank { null },
                            onClick = { showAgeKeyDialog = true },
                        )
                    }
                }

                // Backup now button (custom remote only)
                if (destination == DEST_CUSTOM_REMOTE) {
                    item {
                        Button(
                            onClick = {
                                CloudBackupScheduler.runNow(context)
                                Toast.makeText(context, R.string.cloud_backup_backup_now, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(stringResource(R.string.cloud_backup_backup_now))
                        }
                    }

                    // Last backup status
                    item {
                        val statusText = if (lastBackupTime == 0L) {
                            stringResource(R.string.cloud_backup_last_backup_never)
                        } else {
                            val dateStr = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastBackupTime))
                            if (lastBackupSuccess) {
                                stringResource(R.string.cloud_backup_last_backup_success, dateStr)
                            } else {
                                stringResource(R.string.cloud_backup_last_backup_failed, dateStr)
                            }
                        }
                        StarCloneListItem(
                            headlineText = stringResource(R.string.cloud_backup_last_backup),
                            supportingText = statusText,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // --- Dialogs ---

    if (showRemoteDialog) {
        StarCloneDialog(
            onDismissRequest = { showRemoteDialog = false },
            title = stringResource(R.string.cloud_backup_select_remote),
            text = {
                Column {
                    remotes.forEach { remoteName ->
                        RadioItem(
                            label = remoteName,
                            selected = remoteName == selectedRemote,
                            onClick = {
                                selectedRemote = remoteName
                                prefs.edit().putString(remoteKey, remoteName).apply()
                                showRemoteDialog = false
                                reschedule()
                            },
                        )
                    }
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showRemoteDialog = false },
        )
    }

    if (showPathDialog) {
        var pathValue by remember { mutableStateOf(backupPath) }
        StarCloneDialog(
            onDismissRequest = { showPathDialog = false },
            title = stringResource(R.string.cloud_backup_path),
            text = {
                OutlinedTextField(
                    value = pathValue,
                    onValueChange = { pathValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                backupPath = pathValue
                prefs.edit().putString(pathKey, pathValue).apply()
                showPathDialog = false
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showPathDialog = false },
        )
    }

    if (showFrequencyDialog) {
        StarCloneDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = stringResource(R.string.cloud_backup_frequency),
            text = {
                Column {
                    val options = listOf(
                        CloudBackupScheduler.FREQUENCY_DAILY to stringResource(R.string.cloud_backup_frequency_daily),
                        CloudBackupScheduler.FREQUENCY_WEEKLY to stringResource(R.string.cloud_backup_frequency_weekly),
                        CloudBackupScheduler.FREQUENCY_MONTHLY to stringResource(R.string.cloud_backup_frequency_monthly),
                    )
                    options.forEach { (value, label) ->
                        RadioItem(
                            label = label,
                            selected = frequency == value,
                            onClick = {
                                frequency = value
                                prefs.edit().putInt(frequencyKey, value).apply()
                                showFrequencyDialog = false
                                reschedule()
                            },
                        )
                    }
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showFrequencyDialog = false },
        )
    }

    if (showTimeDialog) {
        var hours by remember { mutableIntStateOf(timeMinutes / 60) }
        var minutes by remember { mutableIntStateOf(timeMinutes % 60) }
        StarCloneDialog(
            onDismissRequest = { showTimeDialog = false },
            title = stringResource(R.string.cloud_backup_time),
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = hours.toString(),
                        onValueChange = { hours = it.toIntOrNull()?.coerceIn(0, 23) ?: hours },
                        label = { Text("HH") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Text(":", modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = String.format("%02d", minutes),
                        onValueChange = { minutes = it.toIntOrNull()?.coerceIn(0, 59) ?: minutes },
                        label = { Text("MM") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                timeMinutes = hours * 60 + minutes
                prefs.edit().putInt(timeKey, timeMinutes).apply()
                showTimeDialog = false
                reschedule()
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showTimeDialog = false },
        )
    }

    if (showRetentionDialog) {
        var retentionValue by remember { mutableStateOf(retention.toString()) }
        StarCloneDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = stringResource(R.string.cloud_backup_retention),
            text = {
                OutlinedTextField(
                    value = retentionValue,
                    onValueChange = { retentionValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                val value = retentionValue.toIntOrNull()?.coerceIn(1, 30) ?: 5
                retention = value
                prefs.edit().putInt(retentionKey, value).apply()
                showRetentionDialog = false
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showRetentionDialog = false },
        )
    }

    if (showEncryptionDialog) {
        StarCloneDialog(
            onDismissRequest = { showEncryptionDialog = false },
            title = stringResource(R.string.cloud_backup_encryption),
            text = {
                Column {
                    RadioItem(
                        label = stringResource(R.string.cloud_backup_encryption_none),
                        selected = encryption == ENCRYPTION_NONE,
                        onClick = {
                            encryption = ENCRYPTION_NONE
                            prefs.edit().putString(encryptionKey, ENCRYPTION_NONE).apply()
                            showEncryptionDialog = false
                        },
                    )
                    RadioItem(
                        label = stringResource(R.string.cloud_backup_encryption_password),
                        selected = encryption == ENCRYPTION_PASSWORD,
                        onClick = {
                            encryption = ENCRYPTION_PASSWORD
                            prefs.edit().putString(encryptionKey, ENCRYPTION_PASSWORD).apply()
                            showEncryptionDialog = false
                            if (!hasPassword.value) showPasswordDialog = true
                        },
                    )
                    RadioItem(
                        label = stringResource(R.string.cloud_backup_encryption_age),
                        description = stringResource(R.string.cloud_backup_encryption_age_desc),
                        selected = encryption == ENCRYPTION_AGE,
                        onClick = {
                            encryption = ENCRYPTION_AGE
                            prefs.edit().putString(encryptionKey, ENCRYPTION_AGE).apply()
                            showEncryptionDialog = false
                            if (agePublicKey.isBlank()) showAgeKeyDialog = true
                        },
                    )
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showEncryptionDialog = false },
        )
    }

    // Password dialog with warnings
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val emptyError = stringResource(R.string.cloud_backup_password_empty)
        val mismatchError = stringResource(R.string.cloud_backup_password_mismatch)

        StarCloneDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = stringResource(R.string.cloud_backup_set_password),
            text = {
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text(stringResource(R.string.cloud_backup_password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        label = { Text(stringResource(R.string.cloud_backup_password_confirm_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                when {
                    password.isBlank() -> error = emptyError
                    password != confirmPassword -> error = mismatchError
                    else -> {
                        showPasswordDialog = false
                        pendingPasswordSave = password
                        if (isUpdatingExistingKey()) {
                            showKeyChangeWarning = true
                        } else {
                            showKeyLossWarning = true
                        }
                    }
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showPasswordDialog = false },
        )
    }

    // Age key dialog with warnings
    if (showAgeKeyDialog) {
        var publicKeyInput by remember {
            mutableStateOf(importedAgePublicKey.ifBlank { agePublicKey })
        }
        var secretKeyInput by remember { mutableStateOf(importedAgeSecretKey) }
        var generatedSecret by remember { mutableStateOf<String?>(null) }
        val clipboardManager = LocalClipboardManager.current

        // Clear imported values so they don't persist for next open
        importedAgePublicKey = ""
        importedAgeSecretKey = ""

        StarCloneDialog(
            onDismissRequest = { showAgeKeyDialog = false },
            title = stringResource(R.string.cloud_backup_encryption_age),
            text = {
                Column {
                    OutlinedTextField(
                        value = publicKeyInput,
                        onValueChange = { publicKeyInput = it },
                        label = { Text(stringResource(R.string.cloud_backup_age_public_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secretKeyInput,
                        onValueChange = { secretKeyInput = it },
                        label = { Text(stringResource(R.string.cloud_backup_age_secret_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val keyPair = AgeEncryption.generateKeyPair()
                            publicKeyInput = keyPair.publicKey
                            secretKeyInput = keyPair.secretKey
                            generatedSecret = keyPair.secretKey
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.cloud_backup_age_generate))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showAgeKeyDialog = false
                            ageKeyFilePicker.launch(arrayOf("*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.cloud_backup_age_import))
                    }
                    if (generatedSecret != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.cloud_backup_age_copy_secret_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedSecret!!))
                                Toast.makeText(context, R.string.cloud_backup_age_copy_secret, Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.cloud_backup_age_copy_secret))
                        }
                    }
                }
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                if (publicKeyInput.isNotBlank()) {
                    showAgeKeyDialog = false
                    pendingAgeSave = publicKeyInput to secretKeyInput
                    if (isUpdatingExistingKey()) {
                        showKeyChangeWarning = true
                    } else {
                        showKeyLossWarning = true
                    }
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = { showAgeKeyDialog = false },
        )
    }

    // First-time key loss warning
    if (showKeyLossWarning) {
        StarCloneDialog(
            onDismissRequest = {
                showKeyLossWarning = false
                pendingPasswordSave = null
                pendingAgeSave = null
            },
            title = stringResource(R.string.cloud_backup_warning_key_loss_title),
            text = {
                Text(
                    text = stringResource(R.string.cloud_backup_warning_key_loss),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                showKeyLossWarning = false
                pendingPasswordSave?.let { commitPasswordSave(it) }
                pendingAgeSave?.let { (pub, sec) -> commitAgeSave(pub, sec) }
                pendingPasswordSave = null
                pendingAgeSave = null
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = {
                showKeyLossWarning = false
                pendingPasswordSave = null
                pendingAgeSave = null
            },
        )
    }

    // Key change warning (updating existing key)
    if (showKeyChangeWarning) {
        StarCloneDialog(
            onDismissRequest = {
                showKeyChangeWarning = false
                pendingPasswordSave = null
                pendingAgeSave = null
            },
            title = stringResource(R.string.cloud_backup_warning_key_change_title),
            text = {
                Text(
                    text = stringResource(R.string.cloud_backup_warning_key_change),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = {
                showKeyChangeWarning = false
                pendingPasswordSave?.let { commitPasswordSave(it) }
                pendingAgeSave?.let { (pub, sec) -> commitAgeSave(pub, sec) }
                pendingPasswordSave = null
                pendingAgeSave = null
                // Immediately create a backup with the new key
                if (destination == DEST_CUSTOM_REMOTE) {
                    CloudBackupScheduler.runNow(context)
                    Toast.makeText(context, R.string.cloud_backup_backup_now, Toast.LENGTH_SHORT).show()
                }
            },
            dismissLabel = stringResource(android.R.string.cancel),
            onDismiss = {
                showKeyChangeWarning = false
                pendingPasswordSave = null
                pendingAgeSave = null
            },
            isDestructive = true,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
