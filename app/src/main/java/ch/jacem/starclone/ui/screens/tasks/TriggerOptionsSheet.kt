package ch.jacem.starclone.ui.screens.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.R
import ch.jacem.starclone.ui.components.StarCloneListItem

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
            headlineText = stringResource(R.string.trigger_option_edit),
            supportingText = stringResource(R.string.trigger_option_edit_desc),
            onClick = onEdit,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.ContentCopy,
            headlineText = stringResource(R.string.trigger_option_duplicate),
            supportingText = stringResource(R.string.trigger_option_duplicate_desc),
            onClick = onCopy,
        )
        StarCloneListItem(
            leadingIcon = Icons.Outlined.Delete,
            headlineText = stringResource(R.string.trigger_option_delete),
            supportingText = stringResource(R.string.trigger_option_delete_desc),
            onClick = onDelete,
        )
    }
}
