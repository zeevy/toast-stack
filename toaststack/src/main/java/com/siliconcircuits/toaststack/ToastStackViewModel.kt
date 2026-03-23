package com.siliconcircuits.toaststack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Extension functions that allow ViewModels to show toasts through
 * the [ToastStack] global singleton with minimal boilerplate.
 *
 * These extensions delegate to [ToastStack.show] and its typed variants,
 * making it natural to trigger toasts from business logic without
 * holding a reference to [ToastStackState]:
 *
 * ```
 * class UploadViewModel : ViewModel() {
 *     fun onUploadComplete() {
 *         showToast("Upload complete", type = ToastType.Success)
 *     }
 *
 *     fun onUploadFailed(error: String) {
 *         showErrorToast(error, title = "Upload Failed")
 *     }
 * }
 * ```
 *
 * For the suspend variant that waits for the toast to be dismissed:
 * ```
 * class DeleteViewModel : ViewModel() {
 *     fun confirmDelete() {
 *         showToastAndAwait("Item deleted", showCloseButton = true) { reason ->
 *             if (reason == DismissReason.Action) undoDelete()
 *         }
 *     }
 * }
 * ```
 */

/**
 * Shows a toast from a ViewModel via the [ToastStack] global singleton.
 *
 * @return A [ToastHandle], or null if no host is registered.
 */
fun ViewModel.showToast(
    message: String,
    title: String? = null,
    type: ToastType = ToastType.Default,
    duration: ToastDuration = ToastDuration.Short,
    position: ToastPosition = ToastPosition.TopCenter,
    showCloseButton: Boolean = false,
    hostTag: String? = null,
    onDismiss: ((DismissReason) -> Unit)? = null
): ToastHandle? = ToastStack.show(
    message = message,
    title = title,
    type = type,
    duration = duration,
    position = position,
    showCloseButton = showCloseButton,
    hostTag = hostTag,
    onDismiss = onDismiss
)

/**
 * Shows a [ToastType.Success] toast from a ViewModel.
 */
fun ViewModel.showSuccessToast(
    message: String,
    title: String? = null,
    hostTag: String? = null
): ToastHandle? = ToastStack.success(message, title, hostTag)

/**
 * Shows a [ToastType.Error] toast from a ViewModel.
 */
fun ViewModel.showErrorToast(
    message: String,
    title: String? = null,
    hostTag: String? = null
): ToastHandle? = ToastStack.error(message, title, hostTag)

/**
 * Shows a [ToastType.Warning] toast from a ViewModel.
 */
fun ViewModel.showWarningToast(
    message: String,
    title: String? = null,
    hostTag: String? = null
): ToastHandle? = ToastStack.warning(message, title, hostTag)

/**
 * Shows a [ToastType.Info] toast from a ViewModel.
 */
fun ViewModel.showInfoToast(
    message: String,
    title: String? = null,
    hostTag: String? = null
): ToastHandle? = ToastStack.info(message, title, hostTag)

/**
 * Shows a [ToastType.Loading] toast from a ViewModel.
 */
fun ViewModel.showLoadingToast(
    message: String,
    title: String? = null,
    hostTag: String? = null
): ToastHandle? = ToastStack.loading(message, title, hostTag)

/**
 * Shows a toast and suspends until it is dismissed, launching the
 * coroutine in the ViewModel's [viewModelScope].
 *
 * The [onResult] callback receives the [DismissReason] when the toast
 * is dismissed. If the ViewModel is cleared while the toast is still
 * visible, the toast is automatically dismissed.
 *
 * @param message The toast message.
 * @param onResult Callback with the dismiss reason.
 */
fun ViewModel.showToastAndAwait(
    message: String,
    title: String? = null,
    type: ToastType = ToastType.Default,
    duration: ToastDuration = ToastDuration.Short,
    showCloseButton: Boolean = false,
    hostTag: String? = null,
    onResult: (DismissReason) -> Unit = {}
) {
    viewModelScope.launch {
        val reason = ToastStack.showAndAwait(
            message = message,
            title = title,
            type = type,
            duration = duration,
            showCloseButton = showCloseButton,
            hostTag = hostTag
        )
        if (reason != null) onResult(reason)
    }
}
