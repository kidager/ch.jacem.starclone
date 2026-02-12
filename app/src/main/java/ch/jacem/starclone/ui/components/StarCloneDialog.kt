package ch.jacem.starclone.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun StarCloneDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    text: @Composable (() -> Unit)? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title?.let { { Text(it) } },
        text = text,
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    colors = if (isDestructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = if (dismissLabel != null && onDismiss != null) {
            {
                OutlinedButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        } else null,
    )
}
