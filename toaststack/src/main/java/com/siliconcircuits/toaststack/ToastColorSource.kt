package com.siliconcircuits.toaststack

/**
 * Where toast colors come from.
 *
 * Set it with [ToastStack.configure]. The default is [AppTheme].
 */
enum class ToastColorSource {
    /**
     * Toasts take their colors and typography from the host app's
     * `MaterialTheme`. Typed toasts map to Material 3 color roles:
     *
     * | Type | Background | Content |
     * | --- | --- | --- |
     * | Default, Loading | inverseSurface | inverseOnSurface |
     * | Success | primaryContainer | onPrimaryContainer |
     * | Info | secondaryContainer | onSecondaryContainer |
     * | Warning | tertiaryContainer | onTertiaryContainer |
     * | Error | errorContainer | onErrorContainer |
     *
     * For the auto overlay the app must also pass its theme wrapper as
     * `theme` in [ToastStack.configure]. Without it the overlay cannot see
     * the app theme, so it falls back to [Library] and logs one warning.
     * A [ToastStackHost] placed by hand inside the app's Compose tree is
     * already under the app theme and needs no wrapper.
     */
    AppTheme,

    /**
     * Toasts use the library's own colors. Default and Loading toasts use
     * the baseline Material 3 inverse surface. Success, Error, Warning
     * and Info use fixed hues (green, red, amber, blue).
     */
    Library,
}
