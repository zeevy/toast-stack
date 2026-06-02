package com.siliconcircuits.toaststack

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
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

                // Avoid double injection if the developer is also placing
                // ToastStackHost manually in their Compose tree.
                val tag = "__toaststack_auto_overlay__"
                val rootView = activity.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
                    ?: return

                // Check if we already added our overlay to this Activity.
                if (rootView.findViewWithTag<ComposeView>(tag) != null) return

                // Wrap the ComposeView in a FrameLayout so we can control touch
                // dispatch (ComposeView is final and cannot be subclassed). The
                // wrapper forwards each touch to the ComposeView and passes through
                // its handled/unhandled result, so touches on a toast are consumed
                // here while touches on empty overlay space fall through to the
                // Activity's own content underneath. See dispatchTouchEvent below.
                val composeView = ComposeView(activity).apply {
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    setContent {
                        ToastStackHost(
                            state = rememberToastStackState(
                                defaultPosition = ToastStack.defaultPosition,
                                defaultDuration = ToastStack.defaultDuration,
                                maxVisible = ToastStack.defaultMaxVisible,
                                defaultSwipeDismiss = ToastStack.defaultSwipeDismiss,
                                defaultAnimation = ToastStack.defaultAnimation,
                                defaultAnimationConfig = ToastStack.defaultAnimationConfig,
                            ),
                            modifier = Modifier.fillMaxSize(),
                            globalStyle = ToastStack.defaultGlobalStyle,
                            contentPadding = ToastStack.defaultContentPadding,
                        )
                    }
                }
                val overlayView = object : FrameLayout(activity) {
                    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
                        // Let Compose process the touch and report back whether it
                        // handled it. AndroidComposeView reports the event as handled
                        // only when it actually lands on a toast (a tap on an action
                        // button, or a swipe-to-dismiss gesture). Touches over the
                        // empty, transparent areas of the overlay are reported as
                        // unhandled.
                        //
                        // Returning that result is what makes the overlay "transparent"
                        // to touches except where a toast is drawn:
                        //  - On a toast: returns true, so this view becomes the touch
                        //    target and the whole down/move/up sequence is delivered to
                        //    Compose (swipe-to-dismiss needs the full sequence), and the
                        //    event is NOT also passed to the content underneath.
                        //  - On empty space: returns false, so the Android view system
                        //    routes the gesture to the Activity's own content below.
                        return super.dispatchTouchEvent(ev)
                    }
                }.apply {
                    this.tag = tag
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

    /**
     * No dependencies on other initializers. This runs as early as
     * possible in the app startup sequence.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
