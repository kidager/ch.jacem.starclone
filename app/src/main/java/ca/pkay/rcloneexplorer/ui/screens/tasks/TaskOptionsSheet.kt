package ca.pkay.rcloneexplorer.ui.screens.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import ca.pkay.rcloneexplorer.Items.Task
import ca.pkay.rcloneexplorer.ui.components.StarCloneListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsSheet(
    task: Task,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        StarCloneListItem(
            leadingIcon = Icons.Outlined.PlayArrow,
            headlineText = "Start",
            supportingText = "Run this task now",
            onClick = onStart,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.Edit,
            headlineText = "Edit",
            supportingText = "Modify task settings",
            onClick = onEdit,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.ContentCopy,
            headlineText = "Duplicate",
            supportingText = "Create a copy of this task",
            onClick = onCopy,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.Delete,
            headlineText = "Delete",
            supportingText = "Remove this task",
            onClick = onDelete,
        )
    }
}
