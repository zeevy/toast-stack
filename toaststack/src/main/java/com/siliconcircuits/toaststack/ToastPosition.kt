package com.siliconcircuits.toaststack

/**
 * Defines the screen anchor point where a toast or group of toasts
 * is displayed.
 *
 * Android supports both left to right (LTR) languages like English and
 * right to left (RTL) languages like Arabic and Hebrew. The "Start" and
 * "End" variants automatically adapt to the current layout direction:
 * - In LTR mode, [TopStart] places the toast at the top left corner.
 * - In RTL mode, [TopStart] places the toast at the top right corner.
 *
 * This mirroring is handled internally by [ToastStackHost] so callers
 * do not need to worry about layout direction when choosing a position.
 *
 * The host groups all active toasts by their position and renders a
 * separate column for each group. For example, if you show one toast
 * at [TopCenter] and another at [BottomEnd], they appear in different
 * corners of the screen simultaneously.
 */
enum class ToastPosition {

    /**
     * Top edge, horizontally centered.
     * This is the default position used when no position is specified.
     */
    TopCenter,

    /**
     * Top edge, aligned to the leading side of the screen.
     * Leading means left in LTR layouts and right in RTL layouts.
     */
    TopStart,

    /**
     * Top edge, aligned to the trailing side of the screen.
     * Trailing means right in LTR layouts and left in RTL layouts.
     */
    TopEnd,

    /**
     * Bottom edge, horizontally centered.
     * Toasts appear above the navigation bar or gesture handle.
     */
    BottomCenter,

    /**
     * Bottom edge, aligned to the leading side of the screen.
     * Leading means left in LTR layouts and right in RTL layouts.
     */
    BottomStart,

    /**
     * Bottom edge, aligned to the trailing side of the screen.
     * Trailing means right in LTR layouts and left in RTL layouts.
     */
    BottomEnd,

    /**
     * Dead center of the screen, both vertically and horizontally.
     * Useful for overlay style notifications that demand attention.
     */
    Center
}
