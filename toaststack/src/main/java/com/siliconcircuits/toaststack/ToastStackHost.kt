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
    contentPadding: PaddingValues = ToastStackDefaults.ContentPadding,
    colorSource: ToastColorSource = ToastStack.colorSource,
) {
    // Capture the application context for string resource resolution.
    // This uses the application context (not the activity), so there is no
    // risk of leaking an activity reference.
    val context = LocalContext.current
    StringResolver.initialize(context)
    RegisterHost(state, tag)

    // When the system "Remove animations" setting is on (animator scale 0),
    // every toast uses a short fade, for users who are sensitive to motion.
    val reduceMotion = remember(context) {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        ) == 0.0f
    }

    // The render list is a superset of state.toasts: it keeps dismissed
    // toasts while their exit animation plays. The visibility map says
    // whether each toast should be visible (true) or animating out (false).
    // An effect keeps them in step, so the host never writes state while it
    // draws.
    val renderList = remember { mutableStateListOf<ToastData>() }
    val visibilityMap = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(state) {
        snapshotFlow { state.toasts }.collect { toasts ->
            syncRenderList(toasts, renderList, visibilityMap, state.shownIds)
        }
    }
    val activeIds = state.toasts.mapTo(HashSet()) { it.id }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        renderList.groupBy { it.position }.forEach { (position, toastsAtPosition) ->
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
                        ToastSlot(
                            toast = toast,
                            index = index,
                            isActive = toast.id in activeIds,
                            reduceMotion = reduceMotion,
                            state = state,
                            renderList = renderList,
                            visibilityMap = visibilityMap,
                            globalStyle = globalStyle,
                            colorSource = colorSource,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Keeps [renderList] and [visibilityMap] in step with [toasts]:
 *  1. A new toast is added invisible, so its enter animation plays, unless
 *     an earlier host already showed it ([shownIds]).
 *  2. An updated toast (progress, action, callback) replaces the old copy.
 *  3. A removed toast is marked invisible so its exit animation plays. It
 *     stays in [renderList] until the animation ends.
 */
internal fun syncRenderList(
    toasts: List<ToastData>,
    renderList: MutableList<ToastData>,
    visibilityMap: MutableMap<String, Boolean>,
    shownIds: Set<String>,
) {
    val ids = toasts.mapTo(HashSet()) { it.id }
    toasts.forEach { toast ->
        val renderIndex = renderList.indexOfFirst { it.id == toast.id }
        if (renderIndex == -1) {
            renderList.add(toast)
            visibilityMap[toast.id] = toast.id in shownIds
        } else if (renderList[renderIndex] != toast) {
            renderList[renderIndex] = toast
        }
    }
    renderList.forEach { toast ->
        if (toast.id !in ids && visibilityMap[toast.id] != false) {
            visibilityMap[toast.id] = false
        }
    }
}

/**
 * Registers [state] with the [ToastStack] singleton while this host is in
 * the composition, so ToastStack.show() finds it, and again on every resume:
 * while this host was in the background, another host with the same tag
 * (the auto overlay of the next Activity) may have taken the entry. Also
 * follows TalkBack (touch exploration), so timers stop while it is on.
 */
@Composable
private fun RegisterHost(state: ToastStackState, tag: String) {
    val context = LocalContext.current
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
}

/** One toast in the host: its show effects, its exit, and its card. */
@Composable
private fun ToastSlot(
    toast: ToastData,
    index: Int,
    isActive: Boolean,
    reduceMotion: Boolean,
    state: ToastStackState,
    renderList: MutableList<ToastData>,
    visibilityMap: MutableMap<String, Boolean>,
    globalStyle: ToastStackStyle?,
    colorSource: ToastColorSource,
) {
    val context = LocalContext.current
    val view = LocalView.current

    // With reduced motion, a short fade; else the toast's animation or the default.
    val animation = if (reduceMotion) ToastAnimation.Fade else toast.animation ?: state.defaultAnimation
    val baseConfig = if (reduceMotion) {
        ToastAnimationConfig(enterDurationMillis = 150, exitDurationMillis = 100)
    } else {
        toast.animationConfig ?: state.defaultAnimationConfig
    }
    val staggeredConfig = if (index > 0 && baseConfig.staggerDelayMillis > 0) {
        baseConfig.copy(enterDurationMillis = baseConfig.enterDurationMillis + index * baseConfig.staggerDelayMillis)
    } else {
        baseConfig
    }

    // Flip a new toast to visible to start the enter animation. Haptic and
    // sound fire on appearance, onShow after the enter animation.
    LaunchedEffect(toast.id) {
        // Dismissed before this frame: let the exit run, do not show it.
        if (state.toasts.none { it.id == toast.id }) return@LaunchedEffect
        visibilityMap[toast.id] = true
        // Shown before a configuration change: no second time.
        if (!state.shownIds.add(toast.id)) return@LaunchedEffect
        if (toast.hapticEnabled) ToastFeedback.haptic(view, toast.type)
        if (toast.soundEnabled) {
            // Preparing a ringtone does file and binder work.
            launch(Dispatchers.IO) { ToastFeedback.playSound(context, toast.soundUri) }
        }
        delay(staggeredConfig.enterDurationMillis.toLong())
        toast.onShow?.invoke()
    }

    val isVisible = visibilityMap[toast.id] ?: false
    // Dismissed and out of view: drop it once the exit animation has ended,
    // with a small buffer so the animation finishes first.
    if (!isActive && !isVisible) {
        LaunchedEffect(toast.id) {
            delay(baseConfig.exitDurationMillis.toLong() + 50L)
            renderList.removeAll { it.id == toast.id }
            visibilityMap.remove(toast.id)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = animation.toEnterTransition(staggeredConfig, toast.position.slideSign),
        exit = animation.toExitTransition(baseConfig, toast.position.slideSign)
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
