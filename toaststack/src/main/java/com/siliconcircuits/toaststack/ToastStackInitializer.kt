package com.siliconcircuits.toaststack

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.startup.Initializer

/**
 * AndroidX Startup [Initializer] that automatically attaches a
 * [ToastStackHost] overlay to every Activity in the application.
 *
 * When this initializer runs (at app startup), it registers an
 * [Application.ActivityLifecycleCallbacks] that injects a
 * [ComposeView] containing [ToastStackHost] into each Activity's
 * root view hierarchy when the Activity is created.
 *
 * This means consumers of the library can call [ToastStack.show]
 * from anywhere without ever adding [ToastStackHost] to their
 * Compose tree manually. Just add the library dependency and go:
 *
 * ```
 * // No setup needed. Just call:
 * ToastStack.success("File saved")
 * ```
 *
 * **Opt out:** If you want to place [ToastStackHost] manually
 * (for example, to use a custom tag or global style), add this
 * to your AndroidManifest.xml to disable auto initialization:
 *
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     tools:node="merge">
 *     <meta-data
 *         android:name="com.siliconcircuits.toaststack.ToastStackInitializer"
 *         tools:node="remove" />
 * </provider>
 * ```
 */
class ToastStackInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val app = context.applicationContext as? Application ?: return
        StringResolver.initialize(context)

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                // Only inject into ComponentActivity (which supports Compose).
                // Plain Activity or AppCompatActivity without Compose are skipped.
                if (activity !is ComponentActivity) return

                // onActivityResumed runs on every resume, so add the overlay only once.
                val tag = "__toaststack_auto_overlay__"
                val rootView = activity.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
                    ?: return
                if (rootView.findViewWithTag<View>(tag) != null) return

                // The overlay covers the screen but takes only touches that land
                // on a toast: AndroidComposeView reports a touch as handled only
                // when a pointer input takes it, so other touches go to the
                // Activity content below.
                val composeView = ComposeView(activity).apply {
                    setContent {
                        // The overlay has its own Compose tree, so it cannot see the
                        // app's MaterialTheme. Wrap the host in the theme the app gave
                        // us in ToastStack.configure(theme = ...). Without it, AppTheme
                        // colors would come from the stock baseline scheme, so fall
                        // back to the library colors and say so once.
                        val theme = ToastStack.theme
                        val colorSource = ToastStack.overlayColorSource()
                        if (colorSource != ToastStack.colorSource) {
                            SideEffect { warnMissingThemeOnce() }
                        }
                        val host: @Composable () -> Unit = {
                            ToastStackHost(
                                // Read the defaults from ToastStack on every toast,
                                // so a later configure() call reaches this overlay.
                                state = rememberToastStackState().apply { followsGlobalDefaults = true },
                                modifier = Modifier.fillMaxSize(),
                                globalStyle = ToastStack.defaultGlobalStyle,
                                contentPadding = ToastStack.defaultContentPadding,
                                colorSource = colorSource,
                            )
                        }
                        if (theme != null) theme(host) else host()
                    }
                }
                // The ComposeView sits in a plain FrameLayout for two reasons.
                // ComponentActivity.setContent reuses child 0 of
                // android.R.id.content when it is a ComposeView, so a bare
                // overlay would be taken over by an Activity that calls
                // setContent after its first resume. And Compose looks for the
                // ViewTree owners from the direct child of android.R.id.content
                // upward, so they go on the wrapper: an Activity that never
                // called setContentView has none higher up.
                val overlayView = FrameLayout(activity).apply {
                    this.tag = tag
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    addView(
                        composeView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
                rootView.addView(overlayView)
                overlayView.bringToFront()
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private var warnedMissingTheme = false

    private fun warnMissingThemeOnce() {
        if (warnedMissingTheme) return
        warnedMissingTheme = true
        Log.w(
            "ToastStack",
            "colorSource is AppTheme but no theme was passed to ToastStack.configure(). " +
                "Falling back to ToastColorSource.Library for the auto overlay.",
        )
    }

    /**
     * No dependencies on other initializers. This runs as early as
     * possible in the app startup sequence.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
