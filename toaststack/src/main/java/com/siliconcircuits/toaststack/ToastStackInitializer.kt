package com.siliconcircuits.toaststack

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Only inject into ComponentActivity (which supports Compose).
                // Plain Activity or AppCompatActivity without Compose are skipped.
                if (activity !is ComponentActivity) return

                // Avoid double injection if the developer is also placing
                // ToastStackHost manually in their Compose tree.
                val tag = "__toaststack_auto_overlay__"
                val rootView = activity.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
                    ?: return

                // Check if we already added our overlay to this Activity.
                if (rootView.findViewWithTag<ComposeView>(tag) != null) return

                val overlayView = ComposeView(activity).apply {
                    this.tag = tag
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    setContent {
                        ToastStackHost(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                rootView.addView(overlayView)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * No dependencies on other initializers. This runs as early as
     * possible in the app startup sequence.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
