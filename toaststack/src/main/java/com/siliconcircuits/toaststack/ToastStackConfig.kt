package com.siliconcircuits.toaststack

/**
 * Configuration DSL for setting global defaults on a [ToastStackHost].
 *
 * Instead of passing individual parameters to [rememberToastStackState],
 * you can use this builder for a more readable configuration block:
 *
 * ```
 * val state = rememberToastStackState(
 *     config = ToastStackConfig {
 *         maxVisible = 3
 *         defaultPosition = ToastPosition.BottomCenter
 *         defaultDuration = ToastDuration.Long
 *         defaultAnimation = ToastAnimation.Fade
 *         defaultSwipeDismiss = SwipeDismissDirection.Both
 *     }
 * )
 * ```
 *
 * All fields have sensible defaults matching [ToastStackState]'s
 * constructor defaults, so you only need to specify what you want
 * to change.
 *
 * @property maxVisible Maximum number of toasts visible at once.
 * @property defaultPosition Default screen position for toasts.
 * @property defaultDuration Default display duration for toasts.
 * @property defaultSwipeDismiss Default swipe direction for toasts.
 * @property defaultAnimation Default enter/exit animation style.
 * @property defaultAnimationConfig Default animation timing and easing.
 */
class ToastStackConfig {
    var maxVisible: Int = 5
    var defaultPosition: ToastPosition = ToastPosition.TopCenter
    var defaultDuration: ToastDuration = ToastDuration.Short
    var defaultSwipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both
    var defaultAnimation: ToastAnimation = ToastAnimation.Slide
    var defaultAnimationConfig: ToastAnimationConfig = ToastAnimationConfig()

    companion object {
        /**
         * Creates a [ToastStackConfig] using a builder lambda.
         *
         * ```
         * val config = ToastStackConfig {
         *     maxVisible = 3
         *     defaultDuration = ToastDuration.Long
         * }
         * ```
         */
        operator fun invoke(block: ToastStackConfig.() -> Unit): ToastStackConfig {
            return ToastStackConfig().apply(block)
        }
    }
}

/**
 * Creates and remembers a [ToastStackState] configured via [ToastStackConfig].
 *
 * This is an alternative to the parameter based [rememberToastStackState]
 * that reads more naturally when many defaults are being set:
 *
 * ```
 * val state = rememberToastStackState(
 *     config = ToastStackConfig {
 *         maxVisible = 3
 *         defaultPosition = ToastPosition.BottomCenter
 *     }
 * )
 * ```
 *
 * @param config The configuration block with desired defaults.
 * @return A remembered [ToastStackState] instance.
 */
@androidx.compose.runtime.Composable
fun rememberToastStackState(config: ToastStackConfig): ToastStackState {
    return rememberToastStackState(
        maxVisible = config.maxVisible,
        defaultPosition = config.defaultPosition,
        defaultDuration = config.defaultDuration,
        defaultSwipeDismiss = config.defaultSwipeDismiss,
        defaultAnimation = config.defaultAnimation,
        defaultAnimationConfig = config.defaultAnimationConfig
    )
}
