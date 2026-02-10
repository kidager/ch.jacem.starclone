package ca.pkay.rcloneexplorer.ui.screens.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import ca.pkay.rcloneexplorer.Items.Trigger
import ca.pkay.rcloneexplorer.ui.components.StarCloneListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerOptionsSheet(
    trigger: Trigger,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        StarCloneListItem(
            leadingIcon = Icons.Outlined.Edit,
            headlineText = "Edit",
            supportingText = "Modify trigger settings",
            onClick = onEdit,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.ContentCopy,
            headlineText = "Duplicate",
            supportingText = "Create a copy of this trigger",
            onClick = onCopy,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.Delete,
            headlineText = "Delete",
            supportingText = "Remove this trigger",
            onClick = onDelete,
        )
    }
}
