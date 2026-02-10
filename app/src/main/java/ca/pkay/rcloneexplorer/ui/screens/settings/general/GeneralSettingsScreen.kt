package ca.pkay.rcloneexplorer.ui.screens.settings.general

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import ca.pkay.rcloneexplorer.AppShortcutsHelper
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.R
import ca.pkay.rcloneexplorer.Rclone
import ca.pkay.rcloneexplorer.ui.components.StarCloneListItem
import de.felixnuesse.extract.settings.language.LanguagePicker

@Composable
fun GeneralSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // Keys
    val showThumbnailsKey = stringResource(R.string.pref_key_show_thumbnails)
    val thumbnailSizeKey = stringResource(R.string.pref_key_thumbnail_size_limit)
    val idleKey = stringResource(R.string.shared_preferences_allow_sync_trigger_while_idle)
    val wifiKey = stringResource(R.string.pref_key_wifi_only_transfers)
    val proxyKey = stringResource(R.string.pref_key_use_proxy)
    val proxyProtocolKey = stringResource(R.string.pref_key_proxy_protocol)
    val proxyHostKey = stringResource(R.string.pref_key_proxy_host)
    val proxyPortKey = stringResource(R.string.pref_key_proxy_port)
    val proxyUserKey = stringResource(R.string.pref_key_proxy_username)
    val proxyPasswordKey = stringResource(R.string.pref_key_proxy_password)

    // State
    var showThumbnails by remember { mutableStateOf(prefs.getBoolean(showThumbnailsKey, true)) }
    val defaultThumbnailSize = remember { context.resources.getInteger(R.integer.default_thumbnail_size_limit).toLong() }
    var thumbnailSize by remember { mutableStateOf(prefs.getLong(thumbnailSizeKey, defaultThumbnailSize)) }
    var allowIdle by remember { mutableStateOf(prefs.getBoolean(idleKey, true)) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean(wifiKey, false)) }
    var useProxy by remember { mutableStateOf(prefs.getBoolean(proxyKey, false)) }
    var proxyProtocol by remember { mutableStateOf(prefs.getString(proxyProtocolKey, "http") ?: "http") }
    var proxyHost by remember { mutableStateOf(prefs.getString(proxyHostKey, "example.com") ?: "example.com") }
    var proxyPort by remember { mutableStateOf(prefs.getInt(proxyPortKey, 8080)) }
    var proxyUser by remember { mutableStateOf(prefs.getString(proxyUserKey, "") ?: "") }
    var proxyPassword by remember { mutableStateOf(prefs.getString(proxyPasswordKey, "") ?: "") }

    // Dialogs
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showThumbnailSizeDialog by remember { mutableStateOf(false) }
    var showProtocolDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<EditDialogConfig?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // App shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.app_shortcuts_settings),
                    supportingText = "Select shortcuts that you can pin on your homescreen",
                    onClick = { showShortcutDialog = true },
                )
            }
        }

        // Language
        item {
            val currentLang = remember { LanguagePicker(context).getCurrentLocale()?.displayLanguage ?: "System" }
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_locale),
                supportingText = currentLang,
                onClick = { LanguagePicker(context).showPicker() },
            )
        }

        // Thumbnails
        item {
            SectionHeader("Thumbnails")
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.show_thumbnails),
                trailingContent = {
                    Switch(checked = showThumbnails, onCheckedChange = null)
                },
                onClick = {
                    showThumbnails = !showThumbnails
                    prefs.edit().putBoolean(showThumbnailsKey, showThumbnails).apply()
                },
            )
        }
        if (showThumbnails) {
            item {
                val sizeMb = thumbnailSize / 1024.0 / 1024.0
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_thumbnails_dlg_title),
                    supportingText = String.format("%.1f MB", sizeMb),
                    onClick = { showThumbnailSizeDialog = true },
                )
            }
        }

        // Background work
        item {
            SectionHeader(stringResource(R.string.preference_category_header_background_work))
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.alarm_while_idle),
                supportingText = stringResource(R.string.alarm_while_idle_summary),
                trailingContent = {
                    Switch(checked = allowIdle, onCheckedChange = null)
                },
                onClick = {
                    allowIdle = !allowIdle
                    prefs.edit().putBoolean(idleKey, allowIdle).apply()
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.transfer_only_on_wifi),
                supportingText = stringResource(R.string.transfer_only_on_wifi_summary),
                trailingContent = {
                    Switch(checked = wifiOnly, onCheckedChange = null)
                },
                onClick = {
                    wifiOnly = !wifiOnly
                    prefs.edit().putBoolean(wifiKey, wifiOnly).apply()
                },
            )
        }

        // Proxy
        item {
            SectionHeader(stringResource(R.string.preference_category_header_proxy))
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_use_proxy_title),
                supportingText = stringResource(R.string.pref_use_proxy_summary),
                trailingContent = {
                    Switch(checked = useProxy, onCheckedChange = null)
                },
                onClick = {
                    useProxy = !useProxy
                    prefs.edit().putBoolean(proxyKey, useProxy).apply()
                },
            )
        }
        if (useProxy) {
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_proxy_protocol_dlg_title),
                    supportingText = "Protocol: $proxyProtocol",
                    onClick = { showProtocolDialog = true },
                )
            }
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_proxy_host_dlg_title),
                    supportingText = proxyHost,
                    onClick = {
                        showEditDialog = EditDialogConfig(
                            title = context.getString(R.string.pref_proxy_host_dlg_title),
                            value = proxyHost,
                            keyboardType = KeyboardType.Uri,
                        ) { newValue ->
                            proxyHost = newValue
                            prefs.edit().putString(proxyHostKey, newValue).apply()
                        }
                    },
                )
            }
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_proxy_port_dlg_title),
                    supportingText = proxyPort.toString(),
                    onClick = {
                        showEditDialog = EditDialogConfig(
                            title = context.getString(R.string.pref_proxy_port_dlg_title),
                            value = proxyPort.toString(),
                            keyboardType = KeyboardType.Number,
                        ) { newValue ->
                            val port = newValue.toIntOrNull() ?: 8080
                            proxyPort = port
                            prefs.edit().putInt(proxyPortKey, port).apply()
                        }
                    },
                )
            }
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_proxy_user),
                    supportingText = proxyUser.ifEmpty { "(not set)" },
                    onClick = {
                        showEditDialog = EditDialogConfig(
                            title = context.getString(R.string.pref_proxy_user),
                            value = proxyUser,
                        ) { newValue ->
                            proxyUser = newValue
                            prefs.edit().putString(proxyUserKey, newValue).apply()
                        }
                    },
                )
            }
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_proxy_password),
                    supportingText = if (proxyPassword.isEmpty()) "(not set)" else "\u2022".repeat(proxyPassword.length.coerceAtMost(8)),
                    onClick = {
                        showEditDialog = EditDialogConfig(
                            title = context.getString(R.string.pref_proxy_password),
                            value = proxyPassword,
                            isPassword = true,
                        ) { newValue ->
                            proxyPassword = newValue
                            prefs.edit().putString(proxyPasswordKey, newValue).apply()
                        }
                    },
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // App shortcuts dialog
    if (showShortcutDialog) {
        AppShortcutDialog(
            onDismiss = { showShortcutDialog = false },
        )
    }

    // Thumbnail size dialog
    if (showThumbnailSizeDialog) {
        ThumbnailSizeDialog(
            currentSize = thumbnailSize,
            onDismiss = { showThumbnailSizeDialog = false },
            onConfirm = { newSize ->
                thumbnailSize = newSize
                prefs.edit().putLong(thumbnailSizeKey, newSize).apply()
                showThumbnailSizeDialog = false
            },
        )
    }

    // Protocol dialog
    if (showProtocolDialog) {
        val protocols = listOf("http", "https", "socks5")
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text(stringResource(R.string.pref_proxy_protocol_dlg_title)) },
            text = {
                Column {
                    protocols.forEach { protocol ->
                        StarCloneListItem(
                            headlineText = protocol,
                            trailingContent = {
                                RadioButton(
                                    selected = proxyProtocol == protocol,
                                    onClick = null,
                                )
                            },
                            onClick = {
                                proxyProtocol = protocol
                                prefs.edit().putString(proxyProtocolKey, protocol).apply()
                                showProtocolDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtocolDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Generic edit text dialog
    showEditDialog?.let { config ->
        EditTextDialog(
            config = config,
            onDismiss = { showEditDialog = null },
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

data class EditDialogConfig(
    val title: String,
    val value: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val isPassword: Boolean = false,
    val onConfirm: (String) -> Unit,
)

@Composable
private fun EditTextDialog(
    config: EditDialogConfig,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(config.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(config.title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = config.keyboardType),
                visualTransformation = if (config.isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                config.onConfirm(text)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ThumbnailSizeDialog(
    currentSize: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf(String.format("%.1f", currentSize / 1024.0 / 1024.0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_thumbnails_dlg_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Size (MB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val mb = text.toDoubleOrNull()
                if (mb != null) {
                    onConfirm((mb * 1024 * 1024).toLong())
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AppShortcutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val shortcutsKey = stringResource(R.string.shared_preferences_app_shortcuts)

    val rclone = remember { Rclone(context) }
    val remotes = remember {
        ArrayList(rclone.remotes).also { list ->
            RemoteItem.prepareDisplay(context, list)
            list.sortWith { a, b -> a.displayName.compareTo(b.displayName) }
        }
    }

    val savedIds = remember { prefs.getStringSet(shortcutsKey, emptySet()) ?: emptySet() }
    val checkedStates = remember {
        remotes.map { remote ->
            val hash = AppShortcutsHelper.getUniqueIdFromString(remote.name)
            savedIds.contains(hash)
        }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_shortcuts_settings_dialog_title)) },
        text = {
            Column {
                remotes.forEachIndexed { index, remote ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = checkedStates[index],
                            onCheckedChange = { checked ->
                                val selectedCount = checkedStates.count { it }
                                if (checked && selectedCount >= 4) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.app_shortcuts_max_toast),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@Checkbox
                                }
                                checkedStates[index] = checked
                            },
                        )
                        Text(
                            text = remote.displayName,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val selectedNames = remotes.filterIndexed { i, _ -> checkedStates[i] }.map { it.name }
                setAppShortcuts(context, prefs, shortcutsKey, remotes, selectedNames)
                onDismiss()
            }) { Text(stringResource(R.string.select)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun setAppShortcuts(
    context: android.content.Context,
    prefs: android.content.SharedPreferences,
    shortcutsKey: String,
    remoteItems: List<RemoteItem>,
    selected: List<String>,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

    val limitedSelected = if (selected.size > 4) selected.subList(0, 4) else selected
    val savedIds = prefs.getStringSet(shortcutsKey, emptySet()) ?: emptySet()
    val updatedIds = HashSet(savedIds)

    val newIds = limitedSelected.map { AppShortcutsHelper.getUniqueIdFromString(it) }
    val removedIds = savedIds.filter { it !in newIds }
    if (removedIds.isNotEmpty()) {
        AppShortcutsHelper.removeAppShortcutIds(context, removedIds)
    }
    updatedIds.removeAll(removedIds.toSet())

    for (name in limitedSelected) {
        val id = AppShortcutsHelper.getUniqueIdFromString(name)
        if (id in updatedIds) continue
        val remote = remoteItems.find { it.name == name } ?: continue
        AppShortcutsHelper.addRemoteToAppShortcuts(context, remote, id)
        updatedIds.add(id)
    }

    prefs.edit().putStringSet(shortcutsKey, updatedIds).apply()
}
