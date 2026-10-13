package com.siliconcircuits.toaststack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

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
 * Compose's Start and End alignments do this mirroring, so callers do
 * not need to worry about layout direction when choosing a position.
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

// How each position is laid out. Each is an exhaustive `when` with no `else`,
// so a new position does not compile until it has its own layout.

/** Where the position's column sits in the host. Start and End follow the layout direction. */
internal val ToastPosition.alignment: Alignment
    get() = when (this) {
        ToastPosition.TopCenter -> Alignment.TopCenter
        ToastPosition.TopStart -> Alignment.TopStart
        ToastPosition.TopEnd -> Alignment.TopEnd
        ToastPosition.BottomCenter -> Alignment.BottomCenter
        ToastPosition.BottomStart -> Alignment.BottomStart
        ToastPosition.BottomEnd -> Alignment.BottomEnd
        ToastPosition.Center -> Alignment.Center
    }

/** How cards line up across the column. */
internal val ToastPosition.horizontalAlignment: Alignment.Horizontal
    get() = when (this) {
        ToastPosition.TopStart, ToastPosition.BottomStart -> Alignment.Start
        ToastPosition.TopEnd, ToastPosition.BottomEnd -> Alignment.End
        ToastPosition.TopCenter, ToastPosition.BottomCenter, ToastPosition.Center -> Alignment.CenterHorizontally
    }

/** Which edge the cards stack from. */
internal val ToastPosition.verticalArrangement: Arrangement.Vertical
    get() = when (this) {
        ToastPosition.TopCenter, ToastPosition.TopStart, ToastPosition.TopEnd -> Arrangement.Top
        ToastPosition.Center -> Arrangement.Center
        ToastPosition.BottomCenter, ToastPosition.BottomStart, ToastPosition.BottomEnd -> Arrangement.Bottom
    }

/** The slide direction: -1 slides in from above, 1 from below, 0 does not slide. */
internal val ToastPosition.slideSign: Int
    get() = when (this) {
        ToastPosition.TopCenter, ToastPosition.TopStart, ToastPosition.TopEnd -> -1
        ToastPosition.Center -> 0
        ToastPosition.BottomCenter, ToastPosition.BottomStart, ToastPosition.BottomEnd -> 1
    }

/** Centered positions fill the width; corner cards wrap their content, at most 320dp. */
internal val ToastPosition.fillsWidth: Boolean
    get() = when (this) {
        ToastPosition.TopCenter, ToastPosition.BottomCenter, ToastPosition.Center -> true
        ToastPosition.TopStart, ToastPosition.TopEnd, ToastPosition.BottomStart, ToastPosition.BottomEnd -> false
    }
