package com.siliconcircuits.toaststack

import androidx.compose.runtime.Composable

/**
 * Builder for constructing complex toasts with a DSL style syntax.
 *
 * Instead of passing many named parameters to [ToastStack.show]:
 * ```
 * ToastStack.show(
 *     message = "File deleted",
 *     title = "Success",
 *     type = ToastType.Success,
 *     duration = ToastDuration.Long,
 *     showCloseButton = true
 * )
 * ```
 *
 * You can use the builder DSL:
 * ```
 * ToastStack.build {
 *     message = "File deleted"
 *     title = "Success"
 *     type = ToastType.Success
 *     duration = ToastDuration.Long
 *     showCloseButton = true
 * }
 * ```
 *
 * The builder is also useful when constructing toasts conditionally
 * or when toast configuration comes from a data source.
 */
@ExperimentalToastStackApi
class ToastBuilder {
    /** Primary text displayed in the toast body. Required. */
    var message: String = ""

    /** Optional bold headline rendered above [message]. */
    var title: String? = null

    /** Semantic type determining default colors and icon. */
    var type: ToastType = ToastType.Default

    /** How long the toast stays visible. */
    var duration: ToastDuration = ToastDuration.Short

    /** Screen position where the toast appears. */
    var position: ToastPosition = ToastPosition.TopCenter

    /** Whether to show the close (X) button. */
    var showCloseButton: Boolean = false

    /** Swipe directions that dismiss the toast. */
    var swipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both

    /** Per toast visual overrides. */
    var style: ToastStackStyle? = null

    /** Enter/exit animation style. */
    var animation: ToastAnimation? = null

    /** Animation timing overrides. */
    var animationConfig: ToastAnimationConfig? = null

    /** Whether to vibrate on appearance. */
    var hapticEnabled: Boolean = false

    /** Whether to play notification sound on appearance. */
    var soundEnabled: Boolean = false

    /** Primary action button label. */
    var actionLabel: String? = null

    /** Primary action button callback. */
    var onAction: (() -> Unit)? = null

    /** Callback when the toast is dismissed. */
    var onDismiss: ((DismissReason) -> Unit)? = null

    /** Callback when the toast becomes visible. */
    var onShow: (() -> Unit)? = null

    /**
     * Builds a [ToastData] from the current builder state.
     */
    internal fun build(): ToastData {
        return ToastData(
            message = message,
            title = title,
            type = type,
            duration = duration,
            position = position,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss,
            style = style,
            animation = animation,
            animationConfig = animationConfig,
            actionLabel = actionLabel,
            onAction = onAction,
            hapticEnabled = hapticEnabled,
            soundEnabled = soundEnabled,
            onShow = onShow,
            onDismiss = onDismiss
        )
    }
}

/**
 * Shows a toast configured via the builder DSL.
 *
 * ```
 * toastState.build {
 *     message = "Connection lost"
 *     type = ToastType.Error
 *     duration = ToastDuration.Long
 *     showCloseButton = true
 *     onDismiss = { reason -> log(reason) }
 * }
 * ```
 *
 * @param block Builder configuration lambda.
 * @return A [ToastHandle] for programmatic control.
 */
@ExperimentalToastStackApi
fun ToastStackState.build(block: ToastBuilder.() -> Unit): ToastHandle {
    val toast = ToastBuilder().apply(block).build()
    return enqueue(toast)
}

/**
 * Shows a toast via the global singleton using the builder DSL.
 *
 * ```
 * ToastStack.build {
 *     message = "Saved"
 *     type = ToastType.Success
 * }
 * ```
 *
 * @param hostTag Optional host to target.
 * @param block Builder configuration lambda.
 * @return A [ToastHandle], or null if no host is registered.
 */
@ExperimentalToastStackApi
fun buildToast(
    hostTag: String? = null,
    block: ToastBuilder.() -> Unit
): ToastHandle? {
    val builder = ToastBuilder().apply(block)
    return ToastStack.show(
        message = builder.message,
        title = builder.title,
        type = builder.type,
        duration = builder.duration,
        position = builder.position,
        showCloseButton = builder.showCloseButton,
        swipeDismiss = builder.swipeDismiss,
        style = builder.style,
        hostTag = hostTag,
        onDismiss = builder.onDismiss
    )
}
