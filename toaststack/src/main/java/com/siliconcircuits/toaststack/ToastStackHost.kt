package com.siliconcircuits.toaststack

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
 * **Exit animation strategy:** When a toast is dismissed, [ToastStackState]
 * removes it from its internal list immediately. To allow the exit animation
 * to play, this host maintains a separate render list that keeps dismissed
 * toasts around with `visible = false`. Once [AnimatedVisibility] finishes
 * the exit transition (after the configured exit duration), the toast is
 * removed from the render list.
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
@Composable
fun ToastStackHost(
    state: ToastStackState = rememberToastStackState(),
    tag: String = DEFAULT_HOST_TAG,
    modifier: Modifier = Modifier,
    globalStyle: ToastStackStyle? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    // Capture the application context for string resource resolution.
    // This runs once per host composition and uses the application context
    // (not the activity) so there is no risk of leaking an activity reference.
    val context = LocalContext.current
    StringResolver.initialize(context)

    // Register this host with the global singleton when it enters composition,
    // and unregister when it leaves. This allows ToastStack.show() to find us.
    // Also listen for TalkBack (touch exploration) state changes so we can
    // pause auto dismiss timers while the screen reader is active. Users
    // relying on TalkBack need time to hear the full announcement before
    // the toast disappears.
    DisposableEffect(tag, state) {
        ToastStack.registerHost(tag, state)

        val accessibilityManager = context.getSystemService(
            android.content.Context.ACCESSIBILITY_SERVICE
        ) as? android.view.accessibility.AccessibilityManager

        val touchExplorationListener =
            android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                if (enabled) state.pauseAll() else state.resumeAll()
            }

        // If TalkBack is already active when the host enters composition,
        // pause all timers immediately.
        if (accessibilityManager?.isTouchExplorationEnabled == true) {
            state.pauseAll()
        }
        accessibilityManager?.addTouchExplorationStateChangeListener(touchExplorationListener)

        onDispose {
            accessibilityManager?.removeTouchExplorationStateChangeListener(touchExplorationListener)
            ToastStack.unregisterHost(tag)
        }
    }

    val layoutDirection = LocalLayoutDirection.current

    // Check if the system's "remove animations" setting is enabled.
    // When active, all toast animations are simplified to a short fade
    // regardless of what the toast or state defaults specify. This ensures
    // users who are sensitive to motion still get a usable experience.
    // The animator duration scale is 0.0 when animations are disabled.
    val animatorScale = android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f
    )
    val reduceMotion = animatorScale == 0.0f
    val reducedConfig = ToastAnimationConfig(enterDurationMillis = 150, exitDurationMillis = 100)

    // --- Render list and visibility tracking ---
    // The render list is a superset of state.toasts. It includes toasts that
    // have been dismissed but whose exit animation is still playing.
    // The visibility map tracks whether each toast should be visible (true)
    // or animating out (false).
    val renderList = remember { mutableStateListOf<ToastData>() }
    val visibilityMap = remember { mutableStateMapOf<String, Boolean>() }

    // Sync the render list with state.toasts on every recomposition.
    // This handles three scenarios:
    //  1. New toasts: added to the render list as invisible (triggers enter animation)
    //  2. Updated toasts: replaced in the render list so changes like progress
    //     values, action labels, or callback updates are reflected in the UI
    //  3. Removed toasts: marked invisible (triggers exit animation)
    val activeIds = state.toasts.map { it.id }.toSet()
    val activeToastsById = state.toasts.associateBy { it.id }

    state.toasts.forEach { toast ->
        val renderIndex = renderList.indexOfFirst { it.id == toast.id }
        if (renderIndex == -1) {
            // New toast: add to render list and start invisible for enter animation.
            renderList.add(toast)
            visibilityMap[toast.id] = false
        } else if (renderList[renderIndex] != toast) {
            // Existing toast was updated (e.g., progress, action, callback).
            // Replace with the latest version so the UI reflects the change.
            renderList[renderIndex] = toast
        }
    }

    // Mark removed toasts as invisible so AnimatedVisibility plays the exit.
    // They stay in renderList until the exit animation duration passes.
    renderList.forEach { toast ->
        if (toast.id !in activeIds && visibilityMap[toast.id] != false) {
            visibilityMap[toast.id] = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        val toastsByPosition = renderList.groupBy { it.position }

        toastsByPosition.forEach { (position, toastsAtPosition) ->
            val boxAlignment = position.toBoxAlignment(layoutDirection)
            val isTopAnchored = position in setOf(
                ToastPosition.TopCenter,
                ToastPosition.TopStart,
                ToastPosition.TopEnd
            )

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
                toastsAtPosition.forEachIndexed { index, toast ->
                    key(toast.id) {
                        val slideSign = when {
                            isTopAnchored -> -1
                            position == ToastPosition.Center -> 0
                            else -> 1
                        }

                        // When reduced motion is active, override the animation to
                        // a simple fade with short duration. Otherwise use the
                        // toast's configured animation or the state default.
                        val animation = if (reduceMotion) {
                            ToastAnimation.Fade
                        } else {
                            toast.animation ?: state.defaultAnimation
                        }
                        val baseConfig = if (reduceMotion) {
                            reducedConfig
                        } else {
                            toast.animationConfig ?: state.defaultAnimationConfig
                        }

                        val staggeredConfig = if (index > 0 && baseConfig.staggerDelayMillis > 0) {
                            val staggerDelay = index * baseConfig.staggerDelayMillis
                            baseConfig.copy(
                                enterDurationMillis = baseConfig.enterDurationMillis + staggerDelay
                            )
                        } else {
                            baseConfig
                        }

                        // Flip new toasts to visible on the next frame to trigger
                        // the enter animation. Also fire haptic/sound feedback
                        // immediately on appearance, and onShow after the enter
                        // animation completes.
                        LaunchedEffect(toast.id) {
                            visibilityMap[toast.id] = true
                            // Haptic and sound fire on appearance (before animation ends)
                            // so the user gets immediate sensory feedback.
                            if (toast.hapticEnabled) {
                                ToastFeedback.vibrate(context, toast.type)
                            }
                            if (toast.soundEnabled) {
                                ToastFeedback.playSound(context, toast.soundUri)
                            }
                            // Wait for the enter animation to finish before firing onShow.
                            delay(staggeredConfig.enterDurationMillis.toLong())
                            toast.onShow?.invoke()
                        }

                        val isVisible = visibilityMap[toast.id] ?: false
                        val isActive = toast.id in activeIds

                        // When a toast becomes invisible (dismissed) and is no longer
                        // in the active state list, wait for the exit animation to
                        // finish then remove it from the render list.
                        if (!isActive && !isVisible) {
                            LaunchedEffect(toast.id) {
                                // Wait for the exit animation to complete before
                                // removing the toast from the render list. Adding
                                // a small buffer ensures the animation finishes
                                // before the composable is torn down.
                                delay(baseConfig.exitDurationMillis.toLong() + 50L)
                                renderList.removeAll { it.id == toast.id }
                                visibilityMap.remove(toast.id)
                            }
                        }

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = animation.toEnterTransition(staggeredConfig, slideSign),
                            exit = animation.toExitTransition(baseConfig, slideSign)
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
