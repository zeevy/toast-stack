package com.siliconcircuits.toaststack

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/**
 * Global singleton entry point for showing toasts from anywhere in the app,
 * including non composable scopes like ViewModels, repositories, callbacks,
 * and background services.
 *
 * In Jetpack Compose, composable functions can only be called from other
 * composable functions. This creates a challenge when you want to show a
 * toast from a ViewModel or a network callback, which are not composable.
 * The [ToastStack] singleton bridges this gap: it holds references to
 * registered [ToastStackHost] instances and routes toast requests to them.
 *
 * **How it works:**
 * 1. You place a [ToastStackHost] composable somewhere in your UI tree
 *    (typically at the root of your Activity's `setContent` block).
 * 2. When the host enters composition, it registers itself with this
 *    singleton via [registerHost].
 * 3. From anywhere in your app, you call `ToastStack.show("message")`
 *    and the singleton forwards the request to the registered host.
 * 4. When the host leaves composition (e.g., the screen is destroyed),
 *    it unregisters itself via [unregisterHost].
 *
 * **Threads:** every call is safe from any thread. The toast is added on
 * the main thread, and callbacks (onDismiss, onShow, actions) run there.
 *
 * **Multiple hosts:** If your app has multiple screens, each with its own
 * host, you can assign a unique [tag] to each one and target specific
 * hosts by passing `hostTag = "myTag"` to [show]. When no tag is provided,
 * the toast is routed to the most recently attached host.
 *
 * Usage from a ViewModel:
 * ```
 * class MyViewModel : ViewModel() {
 *     fun onUploadComplete() {
 *         ToastStack.success("Upload complete")
 *     }
 *
 *     fun onUploadFailed(error: String) {
 *         ToastStack.error(error, title = "Upload Failed")
 *     }
 * }
 * ```
 */
object ToastStack {

    /**
     * Global defaults applied by the auto-initializer when creating the
     * [ToastStackHost] overlay. Call [configure] from your Application's
     * `onCreate` to customize these before any Activity is created.
     */
    @Volatile
    internal var defaultContentPadding: PaddingValues = ToastStackDefaults.ContentPadding
        private set

    @Volatile
    internal var defaultGlobalStyle: ToastStackStyle? = null
        private set

    /** The defaults for new toasts on the auto overlay, replaced in one write by [configure]. */
    @Volatile
    internal var defaults: ToastDefaults = ToastDefaults.Standard
        private set

    // Compose state so an already attached overlay recomposes when
    // configure() is called after the first Activity is shown.
    internal var colorSource: ToastColorSource by mutableStateOf(ToastColorSource.AppTheme)
        private set

    internal var theme: (@Composable (content: @Composable () -> Unit) -> Unit)? by mutableStateOf(null)
        private set

    /**
     * The color source the auto overlay actually uses. [ToastColorSource.AppTheme]
     * needs a [theme] wrapper to see the app theme. Without one the overlay
     * falls back to [ToastColorSource.Library].
     */
    internal fun overlayColorSource(): ToastColorSource =
        if (colorSource == ToastColorSource.AppTheme && theme == null) ToastColorSource.Library else colorSource

    /**
     * Configures global defaults for the auto-initialized [ToastStackHost].
     *
     * Call this from your [Application.onCreate] before any Activity starts:
     * ```
     * class MyApp : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         ToastStack.configure(
     *             contentPadding = PaddingValues(top = 128.dp, start = 16.dp, end = 16.dp),
     *             defaultPosition = ToastPosition.TopCenter,
     *         )
     *     }
     * }
     * ```
     *
     * A later call also reaches an overlay that is already on screen. The
     * toast defaults apply to the next toast shown. A toast already on
     * screen keeps its own duration. [contentPadding] and [globalStyle] are
     * read only when an Activity's overlay is first created.
     *
     * @param contentPadding Space between screen edges and the toast column.
     * @param globalStyle Optional style applied to every toast.
     * @param defaultPosition Default screen position for toasts.
     * @param defaultDuration Default auto-dismiss duration.
     * @param maxVisible Maximum number of toasts visible simultaneously.
     * @param defaultSwipeDismiss Which swipe directions dismiss toasts.
     * @param defaultAnimation Animation style for toast enter/exit.
     * @param defaultAnimationConfig Timing and easing for animations.
     * @param deduplicationWindowMs When more than 0, the same message shown
     *   again within this many milliseconds, while its card is still on
     *   screen, restarts that card's timer and adds one to its repeat count
     *   instead of adding a new card. Defaults to 0 (off).
     * @param showRepeatCount Whether a repeated card shows "(xN)" after the
     *   message. Defaults to true.
     * @param colorSource Where toast colors come from. See [ToastColorSource].
     * @param theme The app's theme composable, for example
     *   `{ content -> MyAppTheme { content() } }`. The auto overlay renders
     *   toasts inside it so they follow the app's colors, typography, dark
     *   mode and dynamic color. Needed only for [ToastColorSource.AppTheme]
     *   with the auto overlay. Two rules:
     *   - The wrapper must only set the theme. Do not put `Surface`,
     *     `Scaffold` or any background in it. The overlay covers the whole
     *     Activity, so a background here hides the app's UI.
     *   - The lambda is kept for the life of the process. Do not capture an
     *     Activity, View, ViewModel or Activity Context in it. Call
     *     [configure] from `Application.onCreate()`.
     */
    fun configure(
        contentPadding: PaddingValues = defaultContentPadding,
        globalStyle: ToastStackStyle? = defaultGlobalStyle,
        defaultPosition: ToastPosition = defaults.position,
        defaultDuration: ToastDuration = defaults.duration,
        maxVisible: Int = defaults.maxVisible,
        defaultSwipeDismiss: SwipeDismissDirection = defaults.swipeDismiss,
        defaultAnimation: ToastAnimation = defaults.animation,
        defaultAnimationConfig: ToastAnimationConfig = defaults.animationConfig,
        deduplicationWindowMs: Long = defaults.deduplicationWindowMs,
        showRepeatCount: Boolean = defaults.showRepeatCount,
        colorSource: ToastColorSource = this.colorSource,
        theme: (@Composable (content: @Composable () -> Unit) -> Unit)? = this.theme,
    ) {
        defaultContentPadding = contentPadding
        defaultGlobalStyle = globalStyle
        require(maxVisible >= 1) { "maxVisible must be at least 1, was $maxVisible" }
        defaults = ToastDefaults(
            maxVisible, defaultPosition, defaultDuration, defaultSwipeDismiss,
            defaultAnimation, defaultAnimationConfig, deduplicationWindowMs, showRepeatCount,
        )
        this.colorSource = colorSource
        this.theme = theme
    }

    /**
     * Thread safe registry of all active [ToastStackHost] instances, keyed
     * by their tag string. Uses [ConcurrentHashMap] because registration
     * can happen from any thread (e.g., composition happens on Main, but
     * we want to be safe regardless).
     */
    private val hostRegistry = ConcurrentHashMap<String, ToastStackState>()

    /**
     * Registered host tags, oldest first. A call without a [hostTag] goes
     * to the last one. A tag moves to the end each time its host registers
     * again, which [ToastStackHost] does on every resume. This makes the
     * common single host case work without any tag management.
     */
    private val recentTags = ArrayList<String>()

    /** Set after the first "no host" warning, so it is logged once per process. */
    @Volatile
    internal var warnedNoHost = false

    /**
     * Registers a host's [state] under the given [tag].
     *
     * Called internally by [ToastStackHost] when it enters the composition
     * tree. After registration, any call to [show] (without a specific tag)
     * will route to this host since it becomes the most recent.
     *
     * @param tag The unique identifier for this host.
     * @param state The [ToastStackState] that manages this host's toasts.
     */
    internal fun registerHost(tag: String, state: ToastStackState) {
        hostRegistry[tag] = state
        state.hostTag = tag
        synchronized(recentTags) {
            recentTags.remove(tag)
            recentTags.add(tag)
        }
    }

    /**
     * Removes a host from the registry.
     *
     * Called internally by [ToastStackHost] when it leaves the composition
     * tree (e.g., the screen it belongs to is removed from the navigation
     * stack). Nothing happens when another host has taken the [tag] since,
     * for example the auto overlay of the Activity that is now on top. If
     * the host is removed, the fallback moves to the host that registered
     * before it.
     *
     * @param tag The unique identifier of the host to remove.
     * @param state The state that host registered with.
     */
    internal fun unregisterHost(tag: String, state: ToastStackState) {
        if (!hostRegistry.remove(tag, state)) return
        synchronized(recentTags) { recentTags.remove(tag) }
    }

    /**
     * Shows a toast on the resolved host and returns a [ToastHandle] for it,
     * or null if no host is currently registered.
     *
     * This is the most flexible entry point. For simpler calls with a
     * specific type, prefer [success], [error], [warning], or [info].
     *
     * @param message The primary text displayed in the toast.
     * @param title Optional bold headline above the message.
     * @param type The semantic [ToastType] determining default colors/icon.
     * @param duration How long the toast stays visible. Null uses the
     *   host's default duration at the time of the call.
     * @param position Where on screen the toast appears. Null uses the
     *   host's default position.
     * @param showCloseButton Whether to show the close (X) button.
     * @param swipeDismiss Which swipe directions dismiss the toast. Null
     *   uses the host's default.
     * @param style Optional per toast visual overrides.
     * @param hostTag Target a specific host by tag. When null, routes to
     *   the most recently attached host.
     * @param animation Enter and exit animation. Null uses the host default.
     * @param animationConfig Animation timing. Null uses the host default.
     * @param customIcon Replaces the type's default icon.
     * @param hapticEnabled Vibrate when the toast appears.
     * @param soundEnabled Play a sound when the toast appears.
     * @param soundUri The sound to play. Null uses the system notification sound.
     * @param priority Where the toast goes when the stack is full.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun show(
        message: String,
        title: String? = null,
        type: ToastType = ToastType.Default,
        duration: ToastDuration? = null,
        position: ToastPosition? = null,
        showCloseButton: Boolean = false,
        swipeDismiss: SwipeDismissDirection? = null,
        style: ToastStackStyle? = null,
        hostTag: String? = null,
        animation: ToastAnimation? = null,
        animationConfig: ToastAnimationConfig? = null,
        customIcon: (@Composable () -> Unit)? = null,
        hapticEnabled: Boolean = false,
        soundEnabled: Boolean = false,
        soundUri: android.net.Uri? = null,
        priority: ToastPriority = ToastPriority.Normal,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? {
        val state = resolveHost(hostTag) ?: return null
        return state.show(
            message = message,
            title = title,
            type = type,
            duration = duration ?: state.defaultDuration,
            position = position ?: state.defaultPosition,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss ?: state.defaultSwipeDismiss,
            style = style,
            animation = animation,
            animationConfig = animationConfig,
            customIcon = customIcon,
            hapticEnabled = hapticEnabled,
            soundEnabled = soundEnabled,
            soundUri = soundUri,
            priority = priority,
            onDismiss = onDismiss
        )
    }

    /**
     * Shows a [ToastType.Success] toast (check icon; colors follow the [ToastColorSource]).
     *
     * @param message The body text (e.g., "File saved").
     * @param title Optional headline above the message.
     * @param hostTag Target a specific host, or null for the default.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun success(
        message: String,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.success(message, title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Error] toast (error icon; colors follow the [ToastColorSource]).
     *
     * @param message The body text (e.g., "Connection failed").
     * @param title Optional headline above the message.
     * @param hostTag Target a specific host, or null for the default.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun error(
        message: String,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.error(message, title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Warning] toast (warning icon; colors follow the [ToastColorSource]).
     *
     * @param message The body text (e.g., "Low battery").
     * @param title Optional headline above the message.
     * @param hostTag Target a specific host, or null for the default.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun warning(
        message: String,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.warning(message, title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Info] toast (info icon; colors follow the [ToastColorSource]).
     *
     * @param message The body text (e.g., "Update available").
     * @param title Optional headline above the message.
     * @param hostTag Target a specific host, or null for the default.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun info(
        message: String,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.info(message, title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Loading] toast with an indeterminate progress
     * indicator. Defaults to [ToastDuration.Indefinite].
     *
     * Use the returned [ToastHandle] to update progress or dismiss:
     * ```
     * val handle = ToastStack.loading("Uploading...")
     * handle?.updateProgress(0.5f)
     * handle?.dismiss()
     * ToastStack.success("Done")
     * ```
     *
     * @return A [ToastHandle], or null if no host was available.
     */
    fun loading(
        message: String,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.loading(message, title, onDismiss = onDismiss)

    /**
     * Shows a toast and suspends until it is dismissed, returning the
     * [DismissReason]. Returns null if no host is registered.
     *
     * If the coroutine is cancelled, the toast is automatically dismissed.
     *
     * @param hostTag Target a specific host, or null for the default.
     * @return The [DismissReason], or null if no host was available.
     */
    suspend fun showAndAwait(
        message: String,
        title: String? = null,
        type: ToastType = ToastType.Default,
        duration: ToastDuration? = null,
        position: ToastPosition? = null,
        showCloseButton: Boolean = false,
        swipeDismiss: SwipeDismissDirection? = null,
        style: ToastStackStyle? = null,
        hostTag: String? = null,
        animation: ToastAnimation? = null,
        animationConfig: ToastAnimationConfig? = null,
        customIcon: (@Composable () -> Unit)? = null,
        hapticEnabled: Boolean = false,
        soundEnabled: Boolean = false,
        soundUri: android.net.Uri? = null,
        priority: ToastPriority = ToastPriority.Normal
    ): DismissReason? {
        val state = resolveHost(hostTag) ?: return null
        return state.showAndAwait(
            message = message,
            title = title,
            type = type,
            duration = duration ?: state.defaultDuration,
            position = position ?: state.defaultPosition,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss ?: state.defaultSwipeDismiss,
            style = style,
            animation = animation,
            animationConfig = animationConfig,
            customIcon = customIcon,
            hapticEnabled = hapticEnabled,
            soundEnabled = soundEnabled,
            soundUri = soundUri,
            priority = priority
        )
    }

    // -- String resource overloads --

    /**
     * Shows a toast using an Android string resource ID.
     *
     * @param messageRes The string resource ID (e.g., `R.string.saved`).
     * @param formatArgs Optional format arguments for the string resource.
     * @return A [ToastHandle], or null if no host was available.
     */
    fun show(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        type: ToastType = ToastType.Default,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? {
        val state = resolveHost(hostTag) ?: return null
        return state.show(messageRes, *formatArgs, title = title, type = type, onDismiss = onDismiss)
    }

    /**
     * Shows a [ToastType.Success] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun success(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.success(messageRes, *formatArgs, title = title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Error] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun error(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.error(messageRes, *formatArgs, title = title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Warning] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun warning(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.warning(messageRes, *formatArgs, title = title, onDismiss = onDismiss)

    /**
     * Shows a [ToastType.Info] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun info(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? = resolveHost(hostTag)?.info(messageRes, *formatArgs, title = title, onDismiss = onDismiss)

    /**
     * Removes a single toast by its [id].
     *
     * The [id] is the string returned by [show] or the typed convenience
     * methods. If the toast was already dismissed or the ID is unknown,
     * this call is silently ignored.
     *
     * @param id The unique toast identifier.
     * @param hostTag Target a specific host, or null for the default.
     */
    fun dismiss(id: String, hostTag: String? = null) {
        findHost(hostTag)?.dismiss(id)
    }

    /**
     * Removes all toasts from the screen.
     *
     * When [hostTag] is provided, only toasts on that specific host are
     * cleared. When [hostTag] is null, every registered host is cleared,
     * removing all toasts everywhere.
     *
     * @param hostTag Optional host to target. Null clears all hosts.
     */
    fun dismissAll(hostTag: String? = null) {
        if (hostTag != null) {
            hostRegistry[hostTag]?.dismissAll()
        } else {
            hostRegistry.values.forEach { it.dismissAll() }
        }
    }

    /**
     * Resolves which [ToastStackState] should receive a toast request.
     *
     * If an explicit [tag] is provided, looks it up in the registry.
     * Otherwise falls back to the most recently registered host. Returns
     * null if no such host is registered, which causes the calling show
     * method to return null and drop the toast. The first drop logs a
     * warning. Calls that do not show a toast use [findHost].
     */
    internal fun resolveHost(tag: String?): ToastStackState? {
        val state = findHost(tag)
        if (state == null && !warnedNoHost) {
            warnedNoHost = true
            val target = if (tag != null) "with tag \"$tag\"" else "yet"
            Log.w(
                "ToastStack",
                "A toast was dropped because no ToastStackHost is registered $target. " +
                    "The auto overlay registers when an Activity resumes. Logged once.",
            )
        }
        return state
    }

    /** Puts the singleton back to its start state. For tests only. */
    internal fun resetForTest() {
        hostRegistry.clear()
        synchronized(recentTags) { recentTags.clear() }
        warnedNoHost = false
        defaults = ToastDefaults.Standard
        defaultContentPadding = ToastStackDefaults.ContentPadding
        defaultGlobalStyle = null
        colorSource = ToastColorSource.AppTheme
        theme = null
    }

    /** The host registered under [tag], or the most recent host when [tag] is null. */
    internal fun findHost(tag: String?): ToastStackState? {
        val key = tag ?: synchronized(recentTags) { recentTags.lastOrNull() }
        return key?.let { hostRegistry[it] }
    }
}
