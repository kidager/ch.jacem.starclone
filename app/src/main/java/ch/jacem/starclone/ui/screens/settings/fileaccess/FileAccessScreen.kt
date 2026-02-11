package ch.jacem.starclone.ui.screens.settings.fileaccess

import android.content.Intent
import android.content.UriPermission
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import ch.jacem.starclone.RemoteConfig.RemoteConfigHelper
import ch.jacem.starclone.Services.RcdService
import ch.jacem.starclone.VirtualContentProvider
import ch.jacem.starclone.ui.components.StarCloneListItem
import io.github.x0b.safdav.file.SafConstants

private const val ANDROID_AUTHORITY = "com.android.externalstorage.documents"

@Composable
fun FileAccessScreen(
    permissions: List<UriPermission>,
    onAddStorage: () -> Unit,
    onRemovePermission: (UriPermission) -> Unit,
    onOpenAllFilesSettings: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val rclone = remember { Rclone(context) }

    val safKey = stringResource(R.string.pref_key_enable_saf)
    val refreshLaKey = stringResource(R.string.pref_key_refresh_local_aliases)
    val storageLocKey = stringResource(R.string.pref_key_accessible_storage_locations)
    val vcpKey = stringResource(R.string.pref_key_enable_vcp)
    val vcpLocalKey = stringResource(R.string.pref_key_vcp_declare_local)
    val vcpGrantAllKey = stringResource(R.string.pref_key_vcp_grant_all)

    var safEnabled by remember { mutableStateOf(prefs.getBoolean(safKey, false)) }
    var refreshLa by remember { mutableStateOf(prefs.getBoolean(refreshLaKey, true)) }
    var vcpEnabled by remember { mutableStateOf(prefs.getBoolean(vcpKey, true)) }
    var vcpDeclareLocal by remember { mutableStateOf(prefs.getBoolean(vcpLocalKey, true)) }
    var vcpGrantAll by remember { mutableStateOf(prefs.getBoolean(vcpGrantAllKey, false)) }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // SAF
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_saf_enable),
                supportingText = stringResource(R.string.pref_saf_summary),
                trailingContent = {
                    Switch(checked = safEnabled, onCheckedChange = null)
                },
                onClick = {
                    safEnabled = !safEnabled
                    prefs.edit().putBoolean(safKey, safEnabled).apply()
                    if (safEnabled) {
                        RemoteConfigHelper.enableSaf(context)
                    } else {
                        rclone.deleteRemote(SafConstants.SAF_REMOTE_NAME)
                    }
                },
            )
        }

        if (safEnabled) {
            items(
                items = permissions,
                key = { it.uri.toString() },
            ) { permission ->
                PermissionRow(
                    permission = permission,
                    onRemove = { onRemovePermission(permission) },
                )
            }
            item {
                Button(
                    onClick = onAddStorage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.pref_saf_add_permission))
                }
            }
        }

        // Refresh local aliases
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_enable_local_alias_title),
                supportingText = stringResource(R.string.pref_enable_local_alias_summary),
                trailingContent = {
                    Switch(checked = refreshLa, onCheckedChange = null)
                },
                onClick = {
                    refreshLa = !refreshLa
                    prefs.edit()
                        .putBoolean(refreshLaKey, refreshLa)
                        .remove(storageLocKey)
                        .apply()
                },
            )
        }

        // All files permission (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            item {
                StarCloneListItem(
                    headlineText = stringResource(R.string.pref_open_all_files_setting_label),
                    supportingText = stringResource(R.string.pref_open_all_files_setting_summary),
                    onClick = onOpenAllFilesSettings,
                )
            }
        }

        // VCP
        item {
            SectionHeader("Content Provider")
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_saf_virtual_content_provider_enable),
                supportingText = stringResource(R.string.pref_saf_virtual_content_provider_summary),
                trailingContent = {
                    Switch(checked = vcpEnabled, onCheckedChange = null)
                },
                onClick = {
                    vcpEnabled = !vcpEnabled
                    prefs.edit().putBoolean(vcpKey, vcpEnabled).apply()
                    if (!vcpEnabled) {
                        val stopIntent = Intent(context, RcdService::class.java).apply {
                            action = RcdService.ACTION_STOP_FOREGROUND
                        }
                        context.startService(stopIntent)
                    }
                    val rootUri = VirtualContentProvider.getRootUri()
                    context.contentResolver.notifyChange(rootUri, null)
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_saf_vcp_local_pretend_enable),
                supportingText = stringResource(R.string.pref_saf_vcp_local_pretend_summary),
                trailingContent = {
                    Switch(checked = vcpDeclareLocal, onCheckedChange = null)
                },
                onClick = {
                    vcpDeclareLocal = !vcpDeclareLocal
                    prefs.edit().putBoolean(vcpLocalKey, vcpDeclareLocal).apply()
                },
            )
        }
        item {
            StarCloneListItem(
                headlineText = stringResource(R.string.pref_saf_vcp_grant_all_enable),
                supportingText = stringResource(R.string.pref_saf_vcp_grant_all_summary),
                trailingContent = {
                    Switch(checked = vcpGrantAll, onCheckedChange = null)
                },
                onClick = {
                    vcpGrantAll = !vcpGrantAll
                    prefs.edit().putBoolean(vcpGrantAllKey, vcpGrantAll).apply()
                },
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
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
private fun PermissionRow(
    permission: UriPermission,
    onRemove: () -> Unit,
) {
    val authority = permission.uri.authority
    var label = permission.uri.path ?: ""
    if (label.startsWith("/tree/")) {
        label = label.substring(6)
    }
    label = when {
        authority != ANDROID_AUTHORITY -> "$authority ($label)"
        label == "primary:" -> "Primary storage ($label)"
        else -> "External storage ($label)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
