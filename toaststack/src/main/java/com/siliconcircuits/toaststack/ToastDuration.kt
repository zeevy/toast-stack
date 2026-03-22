package com.siliconcircuits.toaststack

/**
 * Controls how long a toast remains visible on screen before it is
 * automatically dismissed.
 *
 * Each enum value carries a [millis] property representing the display
 * time in milliseconds. The auto dismiss timer starts as soon as the
 * toast becomes visible and can be paused/resumed by user interaction
 * (e.g., dragging the toast) or lifecycle events (app going to background).
 *
 * If you need a toast that never disappears on its own, use [Indefinite].
 * The user can still dismiss it via swipe or the close button.
 *
 * @property millis The number of milliseconds the toast stays visible.
 *   For [Indefinite], this is set to [Long.MAX_VALUE] which effectively
 *   means the timer never fires.
 */
@ExperimentalToastStackApi
enum class ToastDuration(val millis: Long) {

    /**
     * A brief notification that disappears after 2 seconds.
     * Best for simple confirmations like "Copied to clipboard".
     */
    Short(2_000L),

    /**
     * An extended notification that disappears after 4 seconds.
     * Useful when the message is longer and needs more reading time.
     */
    Long(4_000L),

    /**
     * The toast stays on screen until the user explicitly dismisses it
     * (via swipe, close button, or programmatic call) or until the host
     * composable leaves the composition. The internal timer is effectively
     * disabled by using [kotlin.Long.MAX_VALUE] as the duration.
     */
    Indefinite(kotlin.Long.MAX_VALUE)
}
