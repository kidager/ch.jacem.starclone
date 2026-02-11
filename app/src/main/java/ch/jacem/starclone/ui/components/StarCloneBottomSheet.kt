package ch.jacem.starclone.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomSheetAction(
    val icon: ImageVector,
    val label: String,
    val tint: Color? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarCloneBottomSheet(
    title: String,
    actions: List<BottomSheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            actions.forEach { action ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = action.label,
                            color = action.tint ?: MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            modifier = Modifier.size(24.dp),
                            tint = action.tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.clickable {
                        action.onClick()
                        onDismiss()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
