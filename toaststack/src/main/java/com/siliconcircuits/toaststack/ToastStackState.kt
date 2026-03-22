package com.siliconcircuits.toaststack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central state holder that manages the lifecycle of all active toasts.
 *
 * This class is the core of ToastStack. It maintains an ordered list of
 * visible toasts, enforces the maximum capacity, runs auto dismiss timers,
 * and supports pausing/resuming those timers during user interaction or
 * when the app moves to the background.
 *
 * Internally, the toast list is backed by Compose's `mutableStateListOf`,
 * which is a "snapshot aware" collection. This means that whenever a toast
 * is added, removed, or the list changes in any way, Compose automatically
 * detects the change and re renders the [ToastStackHost] composable that
 * reads [toasts]. You do not need to manually trigger UI updates.
 *
 * The `@Stable` annotation tells the Compose compiler that this object's
 * observable properties don't change in ways that would break comparisons,
 * which helps Compose skip unnecessary recompositions for better performance.
 *
 * @param maxVisible The maximum number of toasts that can be visible at the
 *   same time. When a new toast is shown and the limit is already reached,
 *   the oldest toast is automatically evicted (dismissed with
 *   [DismissReason.Programmatic]) to make room. Defaults to 5.
 * @param defaultPosition The [ToastPosition] used when a toast does not
 *   explicitly specify its own position. Defaults to [ToastPosition.TopCenter].
 * @param defaultDuration The [ToastDuration] used when a toast does not
 *   explicitly specify its own duration. Defaults to [ToastDuration.Short]
 *   (2 seconds).
 * @param defaultSwipeDismiss The [SwipeDismissDirection] used when a toast
 *   does not explicitly specify its own swipe behavior. Defaults to
 *   [SwipeDismissDirection.Both] (swipeable in either direction).
 */
@ExperimentalToastStackApi
@Stable
class ToastStackState(
    val maxVisible: Int = 5,
    val defaultPosition: ToastPosition = ToastPosition.TopCenter,
    val defaultDuration: ToastDuration = ToastDuration.Short,
    val defaultSwipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both,
    val defaultAnimation: ToastAnimation = ToastAnimation.Slide,
    val defaultAnimationConfig: ToastAnimationConfig = ToastAnimationConfig()
) {
    // Snapshot backed list: Compose observes this collection and automatically
    // triggers recomposition when items are added or removed. This is the
    // reactive mechanism that makes toasts appear and disappear on screen.
    private val activeToasts = mutableStateListOf<ToastData>()

    /**
     * Read only snapshot of all currently visible toasts, ordered from
     * oldest (first shown) to newest (most recently shown).
     *
     * Returns a defensive copy so callers cannot accidentally mutate the
     * internal list. The [ToastStackHost] composable reads this property
     * during composition to decide what to render.
     */
    val toasts: List<ToastData> get() = activeToasts.toList()

    // --- Timer bookkeeping ---
    // Each toast with a finite duration has a coroutine based timer that
    // dismisses it when time runs out. To support pause/resume (e.g., when
    // the user drags a toast or the app goes to background), we track:
    //   - timerJobs: the running coroutine Job for each toast's delay
    //   - pausedIds: which toasts currently have their timer paused
    //   - remainingMillis: how many milliseconds were left when the timer was paused
    //   - timerStartedAt: wall clock time when the current timer segment began
    // The mutex protects concurrent access to these maps since pause/resume
    // operations are launched asynchronously.
    private val timerJobs = mutableMapOf<String, Job>()
    private val pausedIds = mutableSetOf<String>()
    private val remainingMillis = mutableMapOf<String, Long>()
    private val timerStartedAt = mutableMapOf<String, Long>()
    private val timerMutex = Mutex()

    // SupervisorJob ensures that if one timer coroutine fails, it does not
    // cancel the entire scope and kill all other running timers. The scope
    // is tied to the Main (UI) dispatcher because timer callbacks modify
    // snapshot state (activeToasts), which must happen on the thread that
    // Compose uses for recomposition. Call [destroy] to cancel the scope
    // and release all resources when this state is no longer needed.
    private val supervisorJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + supervisorJob)

    /**
     * The host tag assigned to this state when it is registered with the
     * [ToastStack] global singleton. This is set internally by
     * [ToastStack.registerHost] and should not be modified by consumers.
     */
    var hostTag: String? = null
        internal set

    /**
     * Displays a new toast and returns its unique ID.
     *
     * The returned ID can be used later to dismiss the toast programmatically
     * via [dismiss]. If the number of visible toasts is already at
     * [maxVisible], the oldest toast is evicted to make room.
     *
     * All parameters except [message] have defaults, so the simplest call
     * is just `show("Hello")`.
     *
     * @param message The primary text displayed in the toast body.
     * @param title Optional bold headline rendered above [message].
     *   When null, only the message is shown.
     * @param type The semantic [ToastType] that determines default colors
     *   and icon. Defaults to [ToastType.Default].
     * @param duration How long the toast remains visible before auto
     *   dismissing. Defaults to [defaultDuration].
     * @param position Where on screen the toast appears.
     *   Defaults to [defaultPosition].
     * @param showCloseButton Whether to render a close (X) button on the
     *   trailing edge of the toast. Defaults to false.
     * @param swipeDismiss Which horizontal directions allow swipe to dismiss.
     *   Defaults to [defaultSwipeDismiss].
     * @param style Optional [ToastStackStyle] overrides applied to this
     *   specific toast. Null fields fall through to the type defaults.
     * @param animation The enter/exit animation style for this toast.
     *   When null, uses [defaultAnimation].
     * @param animationConfig Timing and easing overrides for this toast.
     *   When null, uses [defaultAnimationConfig].
     * @param customIcon Optional composable that replaces the default icon
     *   for the toast's [type]. Useful when you need a non standard icon.
     * @param onDismiss Optional callback invoked when the toast is removed
     *   from the screen, with a [DismissReason] explaining why.
     * @return The unique toast ID string.
     */
    fun show(
        message: String,
        title: String? = null,
        type: ToastType = ToastType.Default,
        duration: ToastDuration = defaultDuration,
        position: ToastPosition = defaultPosition,
        showCloseButton: Boolean = false,
        swipeDismiss: SwipeDismissDirection = defaultSwipeDismiss,
        style: ToastStackStyle? = null,
        animation: ToastAnimation? = null,
        animationConfig: ToastAnimationConfig? = null,
        customIcon: (@Composable () -> Unit)? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): String {
        val toast = ToastData(
            message = message,
            title = title,
            type = type,
            duration = duration,
            position = position,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss,
            style = style,
            animation = animation,
            animationConfig = animationConfig,
            customIcon = customIcon,
            onDismiss = onDismiss
        )
        return enqueue(toast)
    }

    /**
     * Convenience method that shows a [ToastType.Success] toast with a
     * green background and check circle icon.
     *
     * @param message The body text (e.g., "File uploaded successfully").
     * @param title Optional headline above the message.
     * @return The unique toast ID string.
     */
    fun success(
        message: String,
        title: String? = null,
        duration: ToastDuration = defaultDuration,
        position: ToastPosition = defaultPosition,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): String = show(message, title, ToastType.Success, duration, position, onDismiss = onDismiss)

    /**
     * Convenience method that shows a [ToastType.Error] toast with a
     * red background and error icon.
     *
     * @param message The body text (e.g., "Connection failed").
     * @param title Optional headline above the message.
     * @return The unique toast ID string.
     */
    fun error(
        message: String,
        title: String? = null,
        duration: ToastDuration = defaultDuration,
        position: ToastPosition = defaultPosition,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): String = show(message, title, ToastType.Error, duration, position, onDismiss = onDismiss)

    /**
     * Convenience method that shows a [ToastType.Warning] toast with an
     * amber/yellow background and warning triangle icon.
     *
     * @param message The body text (e.g., "Low storage space").
     * @param title Optional headline above the message.
     * @return The unique toast ID string.
     */
    fun warning(
        message: String,
        title: String? = null,
        duration: ToastDuration = defaultDuration,
        position: ToastPosition = defaultPosition,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): String = show(message, title, ToastType.Warning, duration, position, onDismiss = onDismiss)

    /**
     * Convenience method that shows a [ToastType.Info] toast with a
     * blue background and info circle icon.
     *
     * @param message The body text (e.g., "Update available").
     * @param title Optional headline above the message.
     * @return The unique toast ID string.
     */
    fun info(
        message: String,
        title: String? = null,
        duration: ToastDuration = defaultDuration,
        position: ToastPosition = defaultPosition,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): String = show(message, title, ToastType.Info, duration, position, onDismiss = onDismiss)

    /**
     * Adds a pre built [ToastData] to the visible toast stack.
     *
     * This is the shared internal entry point used by [show] and the typed
     * convenience methods. It handles capacity enforcement (evicting the
     * oldest toast when [maxVisible] is reached) and starts the auto dismiss
     * timer for the new toast.
     *
     * @param toast The fully configured toast to add.
     * @return The toast's unique ID.
     */
    internal fun enqueue(toast: ToastData): String {
        // If we are at capacity, remove the oldest toast (the first item
        // in the list) to make room. The evicted toast's onDismiss callback
        // fires with Programmatic reason so the caller knows it was forced out.
        while (activeToasts.size >= maxVisible) {
            val oldest = activeToasts.firstOrNull() ?: break
            removeSilently(oldest.id, DismissReason.Programmatic)
        }
        activeToasts.add(toast)
        scheduleAutoDismiss(toast)
        return toast.id
    }

    /**
     * Removes a single toast from the screen by its [id].
     *
     * If the toast has an `onDismiss` callback, it will be invoked with
     * the provided [reason]. If the [id] does not match any active toast,
     * this call is silently ignored.
     *
     * @param id The unique identifier returned by [show] when the toast was created.
     * @param reason Why the toast is being dismissed. Defaults to
     *   [DismissReason.Programmatic] since this is a code initiated removal.
     */
    fun dismiss(id: String, reason: DismissReason = DismissReason.Programmatic) {
        removeSilently(id, reason)
    }

    /**
     * Removes every active toast at once.
     *
     * Each toast's `onDismiss` callback (if set) is invoked with
     * [DismissReason.Programmatic]. All auto dismiss timers are cancelled.
     * After this call, [toasts] will be empty.
     */
    fun dismissAll() {
        // Take a snapshot of the current list before clearing it, so we
        // can still iterate over the toasts to fire their callbacks.
        val snapshot = activeToasts.toList()
        activeToasts.clear()
        snapshot.forEach { toast ->
            clearTimerState(toast.id)
            toast.onDismiss?.invoke(DismissReason.Programmatic)
        }
    }

    /**
     * Pauses the auto dismiss timer for a specific toast.
     *
     * This is called when the user starts interacting with a toast (e.g.,
     * begins dragging it). The remaining time is saved so that [resumeTimer]
     * can restart from where it left off rather than resetting the full
     * duration.
     *
     * Safe to call multiple times for the same toast. If the timer is
     * already paused, subsequent calls are ignored.
     *
     * @param id The unique identifier of the toast whose timer should pause.
     */
    fun pauseTimer(id: String) {
        coroutineScope.launch {
            timerMutex.withLock {
                if (id in pausedIds) return@launch
                val job = timerJobs[id] ?: return@launch
                val startedAt = timerStartedAt[id] ?: return@launch
                val total = remainingMillis[id] ?: return@launch

                // Calculate how much time has elapsed since the timer started
                // (or was last resumed) and subtract it from the remaining time.
                val elapsed = System.currentTimeMillis() - startedAt
                remainingMillis[id] = (total - elapsed).coerceAtLeast(0L)
                pausedIds.add(id)
                job.cancel()
                timerJobs.remove(id)
            }
        }
    }

    /**
     * Resumes a previously paused auto dismiss timer.
     *
     * Restarts the coroutine delay with whatever time was remaining when
     * [pauseTimer] was called. If the timer was never paused or the toast
     * no longer exists, this call is silently ignored.
     *
     * @param id The unique identifier of the toast whose timer should resume.
     */
    fun resumeTimer(id: String) {
        coroutineScope.launch {
            timerMutex.withLock {
                if (id !in pausedIds) return@launch
                pausedIds.remove(id)
                val leftover = remainingMillis[id] ?: return@launch
                val toast = activeToasts.find { it.id == id } ?: return@launch
                launchTimer(toast, leftover)
            }
        }
    }

    /**
     * Pauses the auto dismiss timer for every active toast.
     *
     * Useful when the app moves to the background (Activity's onPause)
     * so toasts don't expire while the user can't see them. Call
     * [resumeAll] when the app returns to the foreground.
     */
    fun pauseAll() {
        activeToasts.forEach { pauseTimer(it.id) }
    }

    /**
     * Resumes the auto dismiss timer for every paused toast.
     *
     * Typically called when the app returns to the foreground
     * (Activity's onResume) to continue the countdowns that were
     * paused by [pauseAll].
     */
    fun resumeAll() {
        activeToasts.forEach { resumeTimer(it.id) }
    }

    /**
     * Cancels all running timer coroutines, clears the toast list, and
     * shuts down the internal coroutine scope.
     *
     * Call this when the [ToastStackState] is no longer needed to prevent
     * memory leaks from orphaned coroutines that would otherwise keep
     * references to this object alive. After calling destroy, the state
     * should not be used again.
     *
     * When using [rememberToastStackState], the [ToastStackHost] calls
     * this automatically when it leaves the composition tree. If you
     * manage the state manually (e.g., in a ViewModel), call this from
     * `onCleared()`.
     */
    fun destroy() {
        activeToasts.clear()
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        remainingMillis.clear()
        timerStartedAt.clear()
        pausedIds.clear()
        coroutineScope.cancel()
    }

    // --- Private helpers ---

    /**
     * Removes a toast from the internal list, cancels its timer, and
     * invokes its `onDismiss` callback. If no toast matches the given
     * [id], the call is silently ignored.
     */
    private fun removeSilently(id: String, reason: DismissReason) {
        val toast = activeToasts.find { it.id == id } ?: return
        activeToasts.remove(toast)
        clearTimerState(id)
        toast.onDismiss?.invoke(reason)
    }

    /**
     * Starts the auto dismiss timer for a newly added toast.
     * Toasts with [ToastDuration.Indefinite] skip the timer entirely.
     */
    private fun scheduleAutoDismiss(toast: ToastData) {
        if (toast.duration == ToastDuration.Indefinite) return
        launchTimer(toast, toast.duration.millis)
    }

    /**
     * Launches (or relaunches) a coroutine that waits [millis] milliseconds
     * and then dismisses the toast with [DismissReason.Timeout].
     *
     * This is the only place where timer coroutines are created. Both
     * the initial schedule and pause/resume flow converge here.
     */
    private fun launchTimer(toast: ToastData, millis: Long) {
        clearTimerState(toast.id)
        remainingMillis[toast.id] = millis
        timerStartedAt[toast.id] = System.currentTimeMillis()
        timerJobs[toast.id] = coroutineScope.launch {
            delay(millis)
            removeSilently(toast.id, DismissReason.Timeout)
        }
    }

    /**
     * Cancels any running timer coroutine and removes all bookkeeping
     * entries (remaining time, start time, paused flag) for the given toast.
     * Called both when a timer completes normally and when a toast is
     * dismissed by other means.
     */
    private fun clearTimerState(id: String) {
        timerJobs[id]?.cancel()
        timerJobs.remove(id)
        remainingMillis.remove(id)
        timerStartedAt.remove(id)
        pausedIds.remove(id)
    }
}

/**
 * Creates and remembers a [ToastStackState] that is scoped to the current
 * composition.
 *
 * In Jetpack Compose, `remember` preserves a value across recompositions
 * (the process where Compose rebuilds the UI tree when state changes).
 * This means the [ToastStackState] instance is created once and reused,
 * so toasts are not lost during recomposition.
 *
 * However, `remember` does NOT survive configuration changes like device
 * rotation, which destroys and recreates the Activity. If you need toasts
 * to survive rotation, create the [ToastStackState] inside a ViewModel
 * and pass it to [ToastStackHost] directly.
 *
 * @param maxVisible Maximum visible toast count. See [ToastStackState.maxVisible].
 * @param defaultPosition Default screen position. See [ToastStackState.defaultPosition].
 * @param defaultDuration Default display duration. See [ToastStackState.defaultDuration].
 * @param defaultSwipeDismiss Default swipe behavior. See [ToastStackState.defaultSwipeDismiss].
 * @param defaultAnimation Default animation style. See [ToastStackState.defaultAnimation].
 * @param defaultAnimationConfig Default animation timing. See [ToastStackState.defaultAnimationConfig].
 * @return A remembered [ToastStackState] instance.
 */
@ExperimentalToastStackApi
@Composable
fun rememberToastStackState(
    maxVisible: Int = 5,
    defaultPosition: ToastPosition = ToastPosition.TopCenter,
    defaultDuration: ToastDuration = ToastDuration.Short,
    defaultSwipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both,
    defaultAnimation: ToastAnimation = ToastAnimation.Slide,
    defaultAnimationConfig: ToastAnimationConfig = ToastAnimationConfig()
): ToastStackState {
    val state = remember {
        ToastStackState(
            maxVisible = maxVisible,
            defaultPosition = defaultPosition,
            defaultDuration = defaultDuration,
            defaultSwipeDismiss = defaultSwipeDismiss,
            defaultAnimation = defaultAnimation,
            defaultAnimationConfig = defaultAnimationConfig
        )
    }

    // Cancel the internal coroutine scope when the composition that owns
    // this state is permanently disposed. This prevents orphaned timer
    // coroutines from keeping the state alive after the UI is gone.
    DisposableEffect(state) {
        onDispose { state.destroy() }
    }

    return state
}
