package com.siliconcircuits.toaststack

import android.content.Context
import androidx.annotation.StringRes

/**
 * Resolves Android string resources into plain [String] values.
 *
 * This is an internal utility that allows `show(@StringRes)` overloads
 * to convert resource IDs into text without requiring callers to pass
 * a [Context] explicitly. The application context is captured when the
 * first [ToastStackHost] enters composition and stored as a weak
 * reference to the Application (which lives for the entire process,
 * so there is no leak concern).
 *
 * For future Compose Multiplatform support, this class would become
 * an `expect/actual` declaration with platform specific implementations.
 */
internal object StringResolver {

    /**
     * Application context used to resolve string resources.
     * Set by [ToastStackHost] on first composition.
     */
    private var appContext: Context? = null

    /**
     * Stores the application context for string resource resolution.
     * Called once by [ToastStackHost] when it enters composition.
     * Uses the application context (not activity) to avoid leaking
     * activity references.
     */
    internal fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    /**
     * Resolves a string resource ID, with optional format arguments.
     *
     * @param resId The string resource ID (e.g., `R.string.my_message`).
     * @param formatArgs Arguments to substitute into the format string.
     * @return The resolved and formatted string.
     * @throws IllegalStateException if called before [initialize].
     */
    fun resolve(@StringRes resId: Int, vararg formatArgs: Any): String {
        val ctx = appContext
            ?: error("StringResolver not initialized. Ensure ToastStackHost is in the composition.")
        // With no arguments, a "%" in the text is shown as it is.
        return if (formatArgs.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *formatArgs)
    }
}
