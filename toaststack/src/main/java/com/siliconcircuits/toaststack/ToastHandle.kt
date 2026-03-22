package com.siliconcircuits.toaststack

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A handle to an active toast, returned by [ToastStackState.show] and
 * [ToastStack.show].
 *
 * The handle provides a fluent chaining API for configuring callbacks
 * and actions after showing a toast, as well as programmatic control
 * to dismiss or update the toast later.
 *
 * **Chaining example:**
 * ```
 * ToastStack.show("File deleted")
 *     .onDismiss { reason -> log("dismissed: $reason") }
 * ```
 *
 * **Programmatic dismissal:**
 * ```
 * val handle = ToastStack.show("Uploading...")
 * // Later...
 * handle.dismiss()
 * ```
 *
 * **Suspend until dismissed:**
 * ```
 * viewModelScope.launch {
 *     val reason = ToastStack.showAndAwait("Confirm?")
 *     // Resumes when the toast is dismissed
 * }
 * ```
 *
 * The handle holds a reference to the [ToastStackState] that owns the
 * toast. If the state is destroyed (e.g., the host leaves composition),
 * calling [dismiss] is a safe no-op.
 *
 * @property id The unique identifier of the toast. Can be used with
 *   [ToastStackState.dismiss] directly if you prefer the ID based API.
 * @property state The state holder that manages this toast's lifecycle.
 */
@ExperimentalToastStackApi
class ToastHandle internal constructor(
    val id: String,
    private val state: ToastStackState
) {

    /**
     * Dismisses the toast immediately with [DismissReason.Programmatic].
     *
     * Safe to call multiple times or after the toast has already been
     * dismissed by other means (timeout, swipe, close button). Subsequent
     * calls are silently ignored.
     */
    fun dismiss() {
        state.dismiss(id, DismissReason.Programmatic)
    }

    /**
     * Registers a callback that is invoked when this toast becomes
     * visible on screen (after the enter animation completes).
     *
     * This is a chaining method:
     * ```
     * ToastStack.show("Processing")
     *     .onShow { startBackgroundWork() }
     *     .onDismiss { stopBackgroundWork() }
     * ```
     *
     * @param callback Invoked when the toast appears on screen.
     * @return This handle for further chaining.
     */
    fun onShow(callback: () -> Unit): ToastHandle {
        state.updateToastOnShow(id, callback)
        return this
    }

    /**
     * Adds a primary action button to this toast.
     *
     * The action button appears on the trailing edge of the toast card.
     * When the user taps it, the [onClick] callback fires and the toast
     * is automatically dismissed with [DismissReason.Action].
     *
     * This is a chaining method:
     * ```
     * ToastStack.show("File deleted")
     *     .withAction("Undo") { restoreFile() }
     *     .onDismiss { reason -> log(reason) }
     * ```
     *
     * @param label The text displayed on the action button.
     * @param onClick Callback invoked when the button is tapped.
     * @return This handle for further chaining.
     */
    fun withAction(label: String, onClick: () -> Unit): ToastHandle {
        state.updateToastAction(id, label, onClick)
        return this
    }

    /**
     * Updates the progress value on a loading/progress toast.
     *
     * The progress drives a determinate [LinearProgressIndicator] on the
     * toast card. Pass a value between 0f (empty) and 1f (complete).
     *
     * Typically used with [ToastType.Loading] toasts:
     * ```
     * val handle = ToastStack.loading("Uploading...")
     * handle.updateProgress(0.5f)  // 50%
     * handle.updateProgress(1.0f)  // Done
     * handle.dismiss()
     * ```
     *
     * @param progress A value between 0f and 1f representing completion.
     */
    fun updateProgress(progress: Float) {
        state.updateToastProgress(id, progress.coerceIn(0f, 1f))
    }

    /**
     * Updates the progress value and label simultaneously.
     *
     * @param progress A value between 0f and 1f.
     * @param label Descriptive text like "3 of 10 files uploaded".
     */
    fun updateProgress(progress: Float, label: String) {
        state.updateToastProgress(id, progress.coerceIn(0f, 1f), label)
    }

    /**
     * Registers a callback that is invoked when this toast is dismissed,
     * regardless of how it was dismissed (timeout, swipe, close button,
     * or programmatic).
     *
     * This is a chaining method that returns the same handle, allowing
     * fluent API usage:
     * ```
     * ToastStack.show("Saved").onDismiss { reason ->
     *     analytics.log("toast_dismissed", reason.name)
     * }
     * ```
     *
     * If the toast already has an `onDismiss` callback set via the
     * `show()` parameter, this method wraps both callbacks so they
     * are both invoked. The original callback fires first.
     *
     * @param callback A function receiving the [DismissReason] explaining
     *   why the toast was removed.
     * @return This handle for further chaining.
     */
    fun onDismiss(callback: (DismissReason) -> Unit): ToastHandle {
        val toast = state.toasts.find { it.id == id } ?: return this
        val existingCallback = toast.onDismiss

        // Replace the toast in the state with an updated onDismiss that
        // chains the existing callback with the new one.
        val wrappedCallback: (DismissReason) -> Unit = { reason ->
            existingCallback?.invoke(reason)
            callback(reason)
        }
        state.updateToastCallback(id, wrappedCallback)
        return this
    }

    /**
     * Suspends the calling coroutine until this toast is dismissed, then
     * returns the [DismissReason].
     *
     * If the coroutine is cancelled while waiting (e.g., the ViewModel
     * is cleared), the toast is automatically dismissed with
     * [DismissReason.Programmatic].
     *
     * ```
     * viewModelScope.launch {
     *     val reason = handle.await()
     *     if (reason == DismissReason.Timeout) {
     *         // User did not interact with the toast
     *     }
     * }
     * ```
     *
     * @return The [DismissReason] that caused the toast to be removed.
     */
    suspend fun await(): DismissReason {
        return suspendCancellableCoroutine { continuation ->
            // Register a dismiss callback that resumes the coroutine.
            onDismiss { reason ->
                if (continuation.isActive) {
                    continuation.resume(reason)
                }
            }

            // If the coroutine is cancelled externally, dismiss the toast
            // so it doesn't stay on screen with no one waiting for it.
            continuation.invokeOnCancellation {
                dismiss()
            }
        }
    }
}
