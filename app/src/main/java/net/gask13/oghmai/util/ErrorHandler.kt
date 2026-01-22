package net.gask13.oghmai.util

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility object for handling and converting technical errors into user-friendly messages.
 */
object ErrorHandler {

    /**
     * Converts an exception into a user-friendly error message.
     *
     * @param exception The exception to convert
     * @param context Optional context about what operation was being performed
     * @return A user-friendly error message
     */
    fun getErrorMessage(exception: Throwable, context: String? = null): String {
        val baseMessage = when (exception) {
            // Network-related errors
            is UnknownHostException -> "No internet connection. Please check your network settings."
            is SocketTimeoutException -> "The request timed out. Please try again."
            is IOException -> {
                val message = exception.message ?: ""
                when {
                    message.contains("HTTP error: 400") -> "Invalid request. Please check your input."
                    message.contains("HTTP error: 401") -> "Authentication failed. Please log in again."
                    message.contains("HTTP error: 403") -> "Access denied. You don't have permission to perform this action."
                    message.contains("HTTP error: 404") -> "Resource not found."
                    message.contains("HTTP error: 409") -> "Conflict occurred. The item may already exist."
                    message.contains("HTTP error: 429") -> "Too many requests. Please wait a moment and try again."
                    message.contains("HTTP error: 5") -> "Server error. Please try again later."
                    message.contains("Unexpected error") -> "An unexpected error occurred. Please try again."
                    else -> "Network error. Please check your connection and try again."
                }
            }

            // HTTP-specific errors (if not caught by IOException)
            is HttpException -> when (exception.code()) {
                400 -> "Invalid request. Please check your input."
                401 -> "Authentication failed. Please log in again."
                403 -> "Access denied. You don't have permission to perform this action."
                404 -> "Resource not found."
                409 -> "Conflict occurred. The item may already exist."
                429 -> "Too many requests. Please wait a moment and try again."
                in 500..599 -> "Server error. Please try again later."
                else -> "Request failed with error ${exception.code()}."
            }

            // Generic error
            else -> "An unexpected error occurred: ${exception.message ?: "Unknown error"}"
        }

        // Add context if provided
        return if (context != null) {
            "$context: $baseMessage"
        } else {
            baseMessage
        }
    }

    /**
     * Gets a user-friendly message for common operations.
     * This is a convenience method that combines context and error handling.
     */
    fun getOperationErrorMessage(operation: String, exception: Throwable): String {
        val operationContext = when (operation.lowercase()) {
            "fetch", "load" -> "Failed to load data"
            "save" -> "Failed to save"
            "delete" -> "Failed to delete"
            "update" -> "Failed to update"
            "submit" -> "Failed to submit"
            "search" -> "Search failed"
            else -> "Operation failed"
        }

        return getErrorMessage(exception, operationContext)
    }
}
