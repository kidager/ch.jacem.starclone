package ca.pkay.rcloneexplorer.ui.screens.remotes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import ca.pkay.rcloneexplorer.Items.RemoteItem
import ca.pkay.rcloneexplorer.ui.components.BottomSheetAction
import ca.pkay.rcloneexplorer.ui.components.StarCloneBottomSheet

@Composable
fun RemoteOptionsSheet(
    remote: RemoteItem,
    onDismiss: () -> Unit,
    onProperties: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onAddToHomeScreen: (() -> Unit)? = null,
) {
    val errorColor = MaterialTheme.colorScheme.error

    val actions = buildList {
        add(BottomSheetAction(
            icon = Icons.Outlined.Info,
            label = "Properties",
            onClick = onProperties,
        ))
        add(BottomSheetAction(
            icon = Icons.Outlined.Edit,
            label = "Edit",
            onClick = onEdit,
        ))
        add(BottomSheetAction(
            icon = Icons.Outlined.DriveFileRenameOutline,
            label = "Rename",
            onClick = onRename,
        ))
        add(BottomSheetAction(
            icon = Icons.Outlined.PushPin,
            label = if (remote.isPinned) "Unpin from top" else "Pin to top",
            onClick = onPin,
        ))
        if (onAddToHomeScreen != null) {
            add(BottomSheetAction(
                icon = Icons.Outlined.Home,
                label = "Add to Home Screen",
                onClick = onAddToHomeScreen,
            ))
        }
        add(BottomSheetAction(
            icon = Icons.Outlined.Delete,
            label = "Delete",
            tint = errorColor,
            onClick = onDelete,
        ))
    }

    StarCloneBottomSheet(
        title = remote.displayName,
        actions = actions,
        onDismiss = onDismiss,
    )
}
