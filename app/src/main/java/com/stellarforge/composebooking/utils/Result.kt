package com.stellarforge.composebooking.utils

/**
 * A generic sealed class representing the outcome of an asynchronous operation or data transaction.
 *
 * This pattern allows the app to handle Data, Errors, and Loading states in a type-safe manner,
 * avoiding the "Callback Hell" or unchecked exceptions.
 *
 * @param T The type of data held by the Success state.
 */
sealed class Result<out T> {

    /**
     * Indicates that the operation completed successfully.
     * @param data The result data.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Indicates that the operation failed.
     * @param exception The root cause of the failure.
     * @param message An optional user-friendly or debug message (defaults to exception message).
     */
    data class Error(
        val exception: Exception,
        val message: String? = exception.localizedMessage
    ) : Result<Nothing>()

    /**
     * Indicates that the operation is currently in progress.
     * Useful for showing progress bars in the UI.
     */
    object Loading : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Result.Success(data=$data)"
            is Error -> "Result.Error(exception=${exception::class.simpleName}, message=$message)"
            Loading -> "Result.Loading"
        }
    }
}

// --- FUNCTIONAL EXTENSIONS (For cleaner code chains) ---

/**
 * Transforms the data inside a [Result.Success] using the given [transform] function.
 * If the result is Error or Loading, it is returned as-is (type-casted).
 */
inline fun <T, R> Result<T>.mapOnSuccess(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> Result.Loading
    }
}

/**
 * Performs the given [action] only if the result is [Result.Success].
 * Useful for side effects (e.g., logging, navigation) without changing the flow.
 */
inline fun <T> Result<T>.onSuccess(action: (data: T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Performs the given [action] only if the result is [Result.Error].
 */
inline fun <T> Result<T>.onError(action: (exception: Exception, message: String?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception, message)
    }
    return this
}

/**
 * Performs the given [action] only if the result is [Result.Loading].
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}