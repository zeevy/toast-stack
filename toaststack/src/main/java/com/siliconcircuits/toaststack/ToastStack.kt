package com.siliconcircuits.toaststack

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
     * Thread safe registry of all active [ToastStackHost] instances, keyed
     * by their tag string. Uses [ConcurrentHashMap] because registration
     * can happen from any thread (e.g., composition happens on Main, but
     * we want to be safe regardless).
     */
    private val hostRegistry = ConcurrentHashMap<String, ToastStackState>()

    /**
     * Tracks the tag of the most recently registered host. When a caller
     * does not specify a [hostTag], this is used as the fallback target.
     * This makes the common single host case work without any tag management.
     */
    private var mostRecentTag: String? = null

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
        mostRecentTag = tag
    }

    /**
     * Removes a host from the registry.
     *
     * Called internally by [ToastStackHost] when it leaves the composition
     * tree (e.g., the screen it belongs to is removed from the navigation
     * stack). If the removed host was the most recent, the fallback shifts
     * to any remaining host.
     *
     * @param tag The unique identifier of the host to remove.
     */
    internal fun unregisterHost(tag: String) {
        hostRegistry.remove(tag)
        if (mostRecentTag == tag) {
            mostRecentTag = hostRegistry.keys.firstOrNull()
        }
    }

    /**
     * Shows a toast on the resolved host and returns its unique ID, or
     * null if no host is currently registered.
     *
     * This is the most flexible entry point. For simpler calls with a
     * specific type, prefer [success], [error], [warning], or [info].
     *
     * @param message The primary text displayed in the toast.
     * @param title Optional bold headline above the message.
     * @param type The semantic [ToastType] determining default colors/icon.
     * @param duration How long the toast stays visible.
     * @param position Where on screen the toast appears.
     * @param showCloseButton Whether to show the close (X) button.
     * @param swipeDismiss Which swipe directions dismiss the toast.
     * @param style Optional per toast visual overrides.
     * @param hostTag Target a specific host by tag. When null, routes to
     *   the most recently attached host.
     * @param onDismiss Optional callback with the [DismissReason].
     * @return A [ToastHandle], or null if no host was available.
     */
    fun show(
        message: String,
        title: String? = null,
        type: ToastType = ToastType.Default,
        duration: ToastDuration = ToastDuration.Short,
        position: ToastPosition = ToastPosition.TopCenter,
        showCloseButton: Boolean = false,
        swipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both,
        style: ToastStackStyle? = null,
        hostTag: String? = null,
        onDismiss: ((DismissReason) -> Unit)? = null
    ): ToastHandle? {
        val state = resolveHost(hostTag) ?: return null
        return state.show(
            message = message,
            title = title,
            type = type,
            duration = duration,
            position = position,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss,
            style = style,
            onDismiss = onDismiss
        )
    }

    /**
     * Shows a [ToastType.Success] toast (green background, check icon).
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
     * Shows a [ToastType.Error] toast (red background, error icon).
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
     * Shows a [ToastType.Warning] toast (amber background, warning icon).
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
     * Shows a [ToastType.Info] toast (blue background, info icon).
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
     * handle.updateProgress(0.5f)
     * handle.dismiss()
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
        duration: ToastDuration = ToastDuration.Short,
        position: ToastPosition = ToastPosition.TopCenter,
        showCloseButton: Boolean = false,
        swipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both,
        style: ToastStackStyle? = null,
        hostTag: String? = null
    ): DismissReason? {
        val state = resolveHost(hostTag) ?: return null
        return state.showAndAwait(
            message = message,
            title = title,
            type = type,
            duration = duration,
            position = position,
            showCloseButton = showCloseButton,
            swipeDismiss = swipeDismiss,
            style = style
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
        hostTag: String? = null
    ): ToastHandle? = resolveHost(hostTag)?.success(messageRes, *formatArgs, title = title)

    /**
     * Shows a [ToastType.Error] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun error(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null
    ): ToastHandle? = resolveHost(hostTag)?.error(messageRes, *formatArgs, title = title)

    /**
     * Shows a [ToastType.Warning] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun warning(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null
    ): ToastHandle? = resolveHost(hostTag)?.warning(messageRes, *formatArgs, title = title)

    /**
     * Shows a [ToastType.Info] toast using an Android string resource ID.
     * The resource is resolved via [StringResolver] using the application
     * context captured by [ToastStackHost].
     */
    fun info(
        @androidx.annotation.StringRes messageRes: Int,
        vararg formatArgs: Any,
        title: String? = null,
        hostTag: String? = null
    ): ToastHandle? = resolveHost(hostTag)?.info(messageRes, *formatArgs, title = title)

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
        resolveHost(hostTag)?.dismiss(id)
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
     * null if no host is registered at all, which causes the calling
     * method to return null (the toast is silently dropped).
     */
    private fun resolveHost(tag: String?): ToastStackState? {
        if (tag != null) return hostRegistry[tag]
        val fallback = mostRecentTag ?: return null
        return hostRegistry[fallback]
    }
}
