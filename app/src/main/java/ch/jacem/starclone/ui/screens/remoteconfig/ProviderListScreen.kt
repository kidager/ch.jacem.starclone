package ch.jacem.starclone.ui.screens.remoteconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.R
import ch.jacem.starclone.rclone.Provider

@Composable
fun ProviderListScreen(
    providers: List<Provider>,
    searchQuery: String,
    onProviderSelected: (Provider) -> Unit,
    preselection: String? = null,
) {
    val filteredProviders = remember(providers, searchQuery) {
        val sorted = providers.sortedBy { it.name }
        if (searchQuery.isBlank()) sorted
        else sorted.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    var selectedProvider by remember(preselection) {
        mutableStateOf(
            if (preselection != null) {
                providers.find { it.name.equals(preselection, ignoreCase = true) }
            } else null
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedProvider != null) {
                FloatingActionButton(
                    onClick = { selectedProvider?.let { onProviderSelected(it) } },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(R.string.next),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(
                items = filteredProviders,
                key = { it.name },
            ) { provider ->
                ProviderItem(
                    provider = provider,
                    isSelected = selectedProvider?.name == provider.name,
                    onClick = { selectedProvider = provider },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private val DEPRECATED_PROVIDERS = setOf("cache", "google photos", "linkbox", "sharefile")

@Composable
private fun ProviderItem(
    provider: Provider,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDeprecated = provider.name in DEPRECATED_PROVIDERS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(getProviderIcon(provider.name)),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.getNameCapitalized(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isDeprecated) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Deprecated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun getProviderIcon(providerName: String): Int {
    return when (providerName) {
        "box" -> R.drawable.ic_box
        "b2" -> R.drawable.ic_backblaze_b2_black
        "s3" -> R.drawable.ic_amazon
        "dropbox" -> R.drawable.ic_dropbox
        "pcloud" -> R.drawable.ic_pcloud
        "sftp" -> R.drawable.ic_terminal
        "yandex" -> R.drawable.ic_yandex_mono
        "webdav" -> R.drawable.ic_webdav
        "onedrive" -> R.drawable.ic_onedrive
        "alias" -> R.drawable.ic_rclone_logo
        "crypt" -> R.drawable.ic_lock_black
        "azureblob" -> R.drawable.ic_azure_storage_blob_logo
        "cache" -> R.drawable.ic_rclone_logo
        "local" -> R.drawable.ic_tablet_cellphone
        "drive" -> R.drawable.ic_google_drive
        "google photos" -> R.drawable.ic_google_photos
        "union" -> R.drawable.ic_union_24dp
        "mega" -> R.drawable.ic_mega_logo_black
        else -> R.drawable.ic_cloud
    }
}
