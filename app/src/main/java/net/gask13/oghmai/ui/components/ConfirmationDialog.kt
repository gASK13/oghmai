package net.gask13.oghmai.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

sealed class ConfirmDialogRequest(
    val message: String,
    val confirmText: String = "Yes",
    val dismissText: String = "No",
    val onConfirm: () -> Unit
) {
    class Generic(
        action: String,
        item: String,
        onConfirmAction: () -> Unit
    ) : ConfirmDialogRequest(
        message = "Are you sure you want to $action $item?",
        confirmText = action,
        dismissText = "No",
        onConfirm = onConfirmAction
    )
}


@Composable
fun ConfirmDialogHandler(
    dialogRequest: ConfirmDialogRequest?,
    onDismiss: () -> Unit
) {
    if (dialogRequest != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm") },
            text = { Text(dialogRequest.message) },
            confirmButton = {
                TextButton(onClick = {
                    dialogRequest.onConfirm()
                    onDismiss()
                }) {
                    Text(dialogRequest.confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dialogRequest.dismissText)
                }
            }
        )
    }
}
