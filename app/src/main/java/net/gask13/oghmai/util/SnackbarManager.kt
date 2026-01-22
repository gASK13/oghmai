package net.gask13.oghmai.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

/**
 * Unified manager for showing snackbars throughout the app.
 * Provides consistent snackbar display with user-friendly error messages.
 */
object SnackbarManager {

    /**
     * Shows a standard error snackbar with a user-friendly message.
     *
     * @param snackbarHostState The snackbar host state
     * @param exception The exception that occurred
     * @param context Optional context about what operation was being performed
     * @param duration Duration to show the snackbar
     * @param withDismissAction Whether to show a dismiss action
     * @return The snackbar result
     */
    suspend fun showError(
        snackbarHostState: SnackbarHostState,
        exception: Throwable,
        context: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = true
    ): SnackbarResult {
        val message = ErrorHandler.getErrorMessage(exception, context)
        return snackbarHostState.showSnackbar(
            message = message,
            duration = duration,
            withDismissAction = withDismissAction
        )
    }

    /**
     * Shows a standard error snackbar for a specific operation.
     *
     * @param snackbarHostState The snackbar host state
     * @param operation The operation that failed (e.g., "fetch", "save", "delete")
     * @param exception The exception that occurred
     * @param duration Duration to show the snackbar
     * @param withDismissAction Whether to show a dismiss action
     * @return The snackbar result
     */
    suspend fun showOperationError(
        snackbarHostState: SnackbarHostState,
        operation: String,
        exception: Throwable,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = true
    ): SnackbarResult {
        val message = ErrorHandler.getOperationErrorMessage(operation, exception)
        return snackbarHostState.showSnackbar(
            message = message,
            duration = duration,
            withDismissAction = withDismissAction
        )
    }

    /**
     * Shows a success message snackbar.
     *
     * @param snackbarHostState The snackbar host state
     * @param message The success message
     * @param duration Duration to show the snackbar
     * @param withDismissAction Whether to show a dismiss action
     * @return The snackbar result
     */
    suspend fun showSuccess(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = true
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            duration = duration,
            withDismissAction = withDismissAction
        )
    }

    /**
     * Shows an informational message snackbar.
     *
     * @param snackbarHostState The snackbar host state
     * @param message The informational message
     * @param duration Duration to show the snackbar
     * @param withDismissAction Whether to show a dismiss action
     * @return The snackbar result
     */
    suspend fun showInfo(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = true
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            duration = duration,
            withDismissAction = withDismissAction
        )
    }

    /**
     * Shows a snackbar with an action button.
     *
     * @param snackbarHostState The snackbar host state
     * @param message The message to display
     * @param actionLabel The label for the action button
     * @param duration Duration to show the snackbar
     * @param withDismissAction Whether to show a dismiss action
     * @return The snackbar result (ActionPerformed if the action was clicked)
     */
    suspend fun showWithAction(
        snackbarHostState: SnackbarHostState,
        message: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = true
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration,
            withDismissAction = withDismissAction
        )
    }
}
