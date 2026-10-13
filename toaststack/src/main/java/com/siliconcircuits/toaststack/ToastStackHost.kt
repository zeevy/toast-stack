package com.siliconcircuits.toaststack

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * @param colorSource Where the type default colors come from. Defaults to
 *   the value set in [ToastStack.configure].
 */
@Composable
fun ToastStackHost(
    state: ToastStackState = rememberToastStackState(),
    tag: String = DEFAULT_HOST_TAG,
    modifier: Modifier = Modifier,
    globalStyle: ToastStackStyle? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    colorSource: ToastColorSource = ToastStack.colorSource,
) {
    // Capture the application context for string resource resolution.
    // This runs once per host composition and uses the application context
    // (not the activity) so there is no risk of leaking an activity reference.
    val context = LocalContext.current
    val view = LocalView.current
    StringResolver.initialize(context)

    // Register this host with the global singleton when it enters composition,
    // and unregister when it leaves. This allows ToastStack.show() to find us.
    // Register again on every resume: while this host was in the background,
    // another host with the same tag (the auto overlay of the next Activity)
    // may have taken the entry. Also listen for TalkBack (touch exploration) state changes so we can
    // pause auto dismiss timers while the screen reader is active. Users
    // relying on TalkBack need time to hear the full announcement before
    // the toast disappears.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(tag, state, lifecycleOwner) {
        ToastStack.registerHost(tag, state)
        val resumeObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ToastStack.registerHost(tag, state)
        }
        lifecycleOwner.lifecycle.addObserver(resumeObserver)

        val accessibilityManager = context.getSystemService(
            android.content.Context.ACCESSIBILITY_SERVICE
        ) as? android.view.accessibility.AccessibilityManager

        val touchExplorationListener =
            android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                state.pausedForAccessibility = enabled
            }
        state.pausedForAccessibility = accessibilityManager?.isTouchExplorationEnabled == true
        accessibilityManager?.addTouchExplorationStateChangeListener(touchExplorationListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(resumeObserver)
            accessibilityManager?.removeTouchExplorationStateChangeListener(touchExplorationListener)
            ToastStack.unregisterHost(tag, state)
        }
    }

    // Check if the system's "remove animations" setting is enabled.
    // When active, all toast animations are simplified to a short fade
    // regardless of what the toast or state defaults specify. This ensures
    // users who are sensitive to motion still get a usable experience.
    // The animator duration scale is 0.0 when animations are disabled.
    val animatorScale = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    }
    val reduceMotion = animatorScale == 0.0f
    val reducedConfig = ToastAnimationConfig(enterDurationMillis = 150, exitDurationMillis = 100)

    // --- Render list and visibility tracking ---
    // The render list is a superset of state.toasts. It includes toasts that
    // have been dismissed but whose exit animation is still playing.
    // The visibility map tracks whether each toast should be visible (true)
    // or animating out (false).
    val renderList = remember { mutableStateListOf<ToastData>() }
    val visibilityMap = remember { mutableStateMapOf<String, Boolean>() }

    // Keep the render list in step with state.toasts. This runs in an effect,
    // not in the composable body, so the host never writes state it is drawing.
    // It handles three cases:
    //  1. New toasts: added to the render list as invisible (triggers enter animation)
    //  2. Updated toasts: replaced in the render list so changes like progress
    //     values, action labels, or callback updates are reflected in the UI
    //  3. Removed toasts: marked invisible (triggers exit animation)
    LaunchedEffect(state) {
        snapshotFlow { state.toasts }.collect { toasts ->
            val ids = toasts.mapTo(HashSet()) { it.id }
            toasts.forEach { toast ->
                val renderIndex = renderList.indexOfFirst { it.id == toast.id }
                if (renderIndex == -1) {
                    // Start invisible for the enter animation, unless an
                    // earlier host already showed it.
                    renderList.add(toast)
                    visibilityMap[toast.id] = toast.id in state.shownIds
                } else if (renderList[renderIndex] != toast) {
                    renderList[renderIndex] = toast
                }
            }
            // Removed toasts stay in renderList until their exit animation ends.
            renderList.forEach { toast ->
                if (toast.id !in ids && visibilityMap[toast.id] != false) {
                    visibilityMap[toast.id] = false
                }
            }
        }
    }
    val activeIds = state.toasts.mapTo(HashSet()) { it.id }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        val toastsByPosition = renderList.groupBy { it.position }

        toastsByPosition.forEach { (position, toastsAtPosition) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(position.alignment)
                    .padding(contentPadding),
                horizontalAlignment = position.horizontalAlignment,
                verticalArrangement = position.verticalArrangement
            ) {
                toastsAtPosition.forEachIndexed { index, toast ->
                    key(toast.id) {
                        val slideSign = position.slideSign

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
                            // Shown before a configuration change: no second time.
                            if (!state.shownIds.add(toast.id)) return@LaunchedEffect
                            // Haptic and sound fire on appearance (before animation ends)
                            // so the user gets immediate sensory feedback.
                            if (toast.hapticEnabled) {
                                ToastFeedback.haptic(view, toast.type)
                            }
                            if (toast.soundEnabled) {
                                // Preparing a ringtone does file and binder work.
                                launch(Dispatchers.IO) { ToastFeedback.playSound(context, toast.soundUri) }
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
                                colorSource = colorSource,
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
