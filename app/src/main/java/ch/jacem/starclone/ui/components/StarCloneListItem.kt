package ch.jacem.starclone.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun StarCloneListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = MaterialTheme.colorScheme.primary,
    trailingContent: @Composable (() -> Unit)? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {},
) {
    val iconTint = if (isDestructive) MaterialTheme.colorScheme.error else leadingIconTint
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified

    ListItem(
        headlineContent = { Text(headlineText, color = textColor) },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        supportingContent = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        leadingContent = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint,
                )
            }
        } else null,
        trailingContent = trailingContent,
    )
}
