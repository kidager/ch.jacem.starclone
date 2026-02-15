package ch.jacem.starclone.ui.screens.sharing

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.jacem.starclone.Items.RemoteItem

@Composable
fun ShareRemotesScreen(
    remotes: List<RemoteItem>,
    onRemoteClick: (RemoteItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = remotes,
            key = { it.name },
            contentType = { "remote" },
        ) { remote ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemoteClick(remote) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = remote.remoteIcon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = remote.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
