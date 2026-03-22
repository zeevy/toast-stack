package com.siliconcircuits.toaststack

import kotlin.time.Duration

/**
 * Controls how long a toast remains visible on screen before it is
 * automatically dismissed.
 *
 * Three predefined durations cover the most common cases:
 * - [Short] (2 seconds) for quick confirmations
 * - [Long] (4 seconds) for longer messages
 * - [Indefinite] for toasts that must be manually dismissed
 *
 * For arbitrary durations, use [Custom] with a millisecond value or
 * the [invoke] operator with a Kotlin [Duration]:
 * ```
 * // Using the predefined constants
 * ToastStack.show("msg", duration = ToastDuration.Short)
 *
 * // Using a custom millisecond value
 * ToastStack.show("msg", duration = ToastDuration.Custom(3000))
 *
 * // Using Kotlin Duration (requires kotlin.time)
 * ToastStack.show("msg", duration = ToastDuration(3.seconds))
 * ```
 *
 * The auto dismiss timer starts as soon as the toast becomes visible
 * and can be paused/resumed by user interaction (e.g., dragging the
 * toast) or lifecycle events (app going to background).
 *
 * @property millis The number of milliseconds the toast stays visible.
 *   For [Indefinite], this is [Long.MAX_VALUE] which effectively means
 *   the timer never fires.
 */
@ExperimentalToastStackApi
sealed class ToastDuration(val millis: kotlin.Long) {

    /**
     * A brief notification that disappears after 2 seconds.
     * Best for simple confirmations like "Copied to clipboard".
     */
    data object Short : ToastDuration(2_000L)

    /**
     * An extended notification that disappears after 4 seconds.
     * Useful when the message is longer and needs more reading time.
     */
    data object Long : ToastDuration(4_000L)

    /**
     * The toast stays on screen until the user explicitly dismisses it
     * (via swipe, close button, or programmatic call) or until the host
     * composable leaves the composition. The internal timer is effectively
     * disabled by using [kotlin.Long.MAX_VALUE] as the duration.
     */
    data object Indefinite : ToastDuration(kotlin.Long.MAX_VALUE)

    /**
     * A toast duration with an arbitrary number of milliseconds.
     * Use this when the predefined [Short], [Long], and [Indefinite]
     * values don't fit your use case.
     *
     * @param millis The display time in milliseconds. Must be positive.
     */
    class Custom(millis: kotlin.Long) : ToastDuration(millis) {
        init {
            require(millis > 0) { "Custom duration must be positive, was $millis" }
        }
    }

    companion object {
        /**
         * Creates a [ToastDuration] from a Kotlin [Duration].
         *
         * This lets you write `ToastDuration(3.seconds)` or
         * `ToastDuration(500.milliseconds)` for a natural, readable API.
         *
         * [Duration.INFINITE] maps to [Indefinite]. All other values are
         * converted to milliseconds and wrapped in [Custom].
         *
         * @param duration The Kotlin time duration. Must be positive or infinite.
         * @return A [ToastDuration] matching the given duration.
         */
        operator fun invoke(duration: Duration): ToastDuration {
            if (duration.isInfinite()) return Indefinite
            return Custom(duration.inWholeMilliseconds)
        }
    }
}
