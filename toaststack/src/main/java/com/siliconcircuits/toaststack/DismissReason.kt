package com.siliconcircuits.toaststack

/**
 * Describes the reason why a toast was removed from the screen.
 *
 * When a toast is dismissed, the library passes one of these values to
 * the optional `onDismiss` callback that was provided when the toast was
 * created. This lets the caller react differently depending on how the
 * toast was dismissed. For example, you might want to log analytics for
 * user initiated dismissals but ignore automatic timeouts.
 *
 * Example usage:
 * ```
 * toastState.show("File deleted", onDismiss = { reason ->
 *     when (reason) {
 *         DismissReason.Timeout -> { /* No action needed */ }
 *         DismissReason.Swipe -> { /* User actively dismissed */ }
 *         DismissReason.CloseButton -> { /* User tapped close */ }
 *         DismissReason.Programmatic -> { /* Code called dismiss() */ }
 *     }
 * })
 * ```
 */
@ExperimentalToastStackApi
enum class DismissReason {

    /**
     * The toast's auto dismiss timer expired. This happens after the
     * duration specified by [ToastDuration] (e.g., 2 seconds for Short,
     * 4 seconds for Long). Toasts with [ToastDuration.Indefinite] will
     * never receive this reason.
     */
    Timeout,

    /**
     * The user dismissed the toast by swiping it horizontally off the
     * screen. The swipe direction must match the toast's configured
     * [SwipeDismissDirection] for this to trigger.
     */
    Swipe,

    /**
     * The user tapped the close button (the X icon) on the trailing
     * edge of the toast. This button only appears when `showCloseButton`
     * is set to true in the toast configuration.
     */
    CloseButton,

    /**
     * The toast was removed by code rather than by the user. This
     * happens when [ToastStackState.dismiss] or [ToastStackState.dismissAll]
     * is called, or when a toast is evicted because the maximum visible
     * limit ([ToastStackState.maxVisible]) was reached and a new toast
     * needed to be shown.
     */
    Programmatic
}
