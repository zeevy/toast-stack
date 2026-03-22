package com.siliconcircuits.toaststack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Fallback tag assigned when the caller does not provide an explicit host tag.
 * Uses a prefix unlikely to collide with user chosen tags.
 */
private const val DEFAULT_HOST_TAG = "__toaststack_default__"

/**
 * Renders active toasts from [state] as an overlay layer on top of your content.
 *
 * Place this composable at the root of your screen or activity content.
 * It fills the available space and positions each toast according to its
 * [ToastPosition]. The host automatically accounts for:
 * - System bar insets (status bar, navigation bar, display cutouts)
 * - The software keyboard (IME), so bottom positioned toasts shift upward
 *
 * On composition, the host registers itself with the [ToastStack] global
 * singleton so that non composable callers (ViewModels, repositories,
 * callbacks) can show toasts via [ToastStack.show]. When this composable
 * leaves the tree (e.g., the screen is removed), it unregisters automatically.
 *
 * Multiple hosts can coexist in the same app by supplying distinct [tag]
 * values. Toasts routed without a tag land on the most recently attached host.
 *
 * @param state The state holder that owns the toast list. Use
 *   [rememberToastStackState] for composition scoped state, or provide a
 *   ViewModel owned instance to survive configuration changes like rotation.
 * @param tag Unique identifier used by [ToastStack] to route toasts to this
 *   specific host. Defaults to an internal tag when only one host exists.
 * @param globalStyle Optional [ToastStackStyle] applied to every toast rendered
 *   by this host. Per toast overrides in [ToastData.style] take priority.
 * @param contentPadding Space between the screen edges and the toast column.
 *   Defaults to 16dp horizontal / 8dp vertical.
 */
@ExperimentalToastStackApi
@Composable
fun ToastStackHost(
    state: ToastStackState = rememberToastStackState(),
    tag: String = DEFAULT_HOST_TAG,
    modifier: Modifier = Modifier,
    globalStyle: ToastStackStyle? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    // Register this host with the global singleton when it enters composition,
    // and unregister when it leaves. This allows ToastStack.show() to find us.
    DisposableEffect(tag, state) {
        ToastStack.registerHost(tag, state)
        onDispose { ToastStack.unregisterHost(tag) }
    }

    // Read the current layout direction (LTR or RTL) so we can mirror
    // Start/End positions correctly for right to left languages like Arabic.
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // systemBarsPadding ensures toasts don't overlap the status bar at
            // the top or the navigation bar/gesture handle at the bottom.
            .systemBarsPadding()
            // imePadding shifts the entire box upward when the software keyboard
            // is open, preventing bottom positioned toasts from hiding behind it.
            .imePadding()
    ) {
        // Group all active toasts by their position so we can render a separate
        // column for each anchor point (e.g., TopCenter, BottomEnd, Center).
        val toastsByPosition = state.toasts.groupBy { it.position }

        toastsByPosition.forEach { (position, toastsAtPosition) ->
            // Convert the logical position to a Box alignment, accounting for
            // RTL layout direction (Start becomes End and vice versa).
            val boxAlignment = position.toBoxAlignment(layoutDirection)

            // Determine whether this position is at the top of the screen.
            // This affects the slide animation direction: top positioned toasts
            // slide in from above, bottom positioned toasts slide in from below.
            val isTopAnchored = position in setOf(
                ToastPosition.TopCenter,
                ToastPosition.TopStart,
                ToastPosition.TopEnd
            )

            // Stack toasts from the edge inward: top positions grow downward,
            // bottom positions grow upward, center positions expand from middle.
            val verticalArrangement = when {
                isTopAnchored -> Arrangement.Top
                position == ToastPosition.Center -> Arrangement.Center
                else -> Arrangement.Bottom
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(boxAlignment)
                    .padding(contentPadding),
                horizontalAlignment = position.toHorizontalAlignment(layoutDirection),
                verticalArrangement = verticalArrangement
            ) {
                toastsAtPosition.forEach { toast ->
                    // key() tells Compose to track this item by its unique toast ID.
                    // Without this, removing a middle toast could cause the wrong
                    // item to animate out.
                    key(toast.id) {
                        // slideSign controls animation direction:
                        //  -1 means "slide down from above" (negative Y offset enters from top)
                        //  +1 means "slide up from below" (positive Y offset enters from bottom)
                        val slideSign = if (isTopAnchored) -1 else 1

                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { height -> slideSign * height }
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { height -> slideSign * height }
                            ) + fadeOut()
                        ) {
                            ToastItem(
                                toast = toast,
                                globalStyle = globalStyle,
                                onDismiss = { reason -> state.dismiss(toast.id, reason) },
                                onPauseTimer = { state.pauseTimer(toast.id) },
                                onResumeTimer = { state.resumeTimer(toast.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts a [ToastPosition] to a [Box] [Alignment] value, flipping
 * Start and End when the layout direction is right to left (RTL).
 *
 * Android supports both left to right (LTR) languages like English and
 * right to left (RTL) languages like Arabic and Hebrew. "Start" means
 * the leading edge (left in LTR, right in RTL) and "End" means the
 * trailing edge. This function resolves the logical Start/End to the
 * correct physical alignment so the toast appears on the expected side.
 *
 * @param layoutDirection The current [LayoutDirection] from the composition,
 *   typically provided by [LocalLayoutDirection].
 * @return The physical [Alignment] that places the toast column at the
 *   correct screen corner or edge.
 */
@ExperimentalToastStackApi
private fun ToastPosition.toBoxAlignment(layoutDirection: LayoutDirection): Alignment {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    return when (this) {
        ToastPosition.TopCenter -> Alignment.TopCenter
        ToastPosition.TopStart -> if (isRtl) Alignment.TopEnd else Alignment.TopStart
        ToastPosition.TopEnd -> if (isRtl) Alignment.TopStart else Alignment.TopEnd
        ToastPosition.BottomCenter -> Alignment.BottomCenter
        ToastPosition.BottomStart -> if (isRtl) Alignment.BottomEnd else Alignment.BottomStart
        ToastPosition.BottomEnd -> if (isRtl) Alignment.BottomStart else Alignment.BottomEnd
        ToastPosition.Center -> Alignment.Center
    }
}

/**
 * Determines how toast cards are horizontally aligned within their column.
 *
 * For Start positions the column aligns its children to the leading edge,
 * for End positions to the trailing edge, and for Center/Top/Bottom center
 * positions the children are centered horizontally. RTL flips Start and End
 * so the visual result matches the reading direction.
 *
 * @param layoutDirection The current [LayoutDirection] from the composition.
 * @return The [Alignment.Horizontal] value to pass to the [Column]'s
 *   `horizontalAlignment` parameter.
 */
@ExperimentalToastStackApi
private fun ToastPosition.toHorizontalAlignment(layoutDirection: LayoutDirection): Alignment.Horizontal {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    return when (this) {
        ToastPosition.TopStart, ToastPosition.BottomStart ->
            if (isRtl) Alignment.End else Alignment.Start
        ToastPosition.TopEnd, ToastPosition.BottomEnd ->
            if (isRtl) Alignment.Start else Alignment.End
        else -> Alignment.CenterHorizontally
    }
}
