package com.siliconcircuits.toaststack

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** An Activity with plain View content that counts the taps reaching it. */
class PlainContentActivity : ComponentActivity() {
    var taps = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this).apply { setOnClickListener { taps++ } })
    }
}

/** An app screen that places its own ToastStackHost with the default tag. */
class HandPlacedHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ToastStackHost() }
    }
}

/** Like a Dagger multibinding factory: it knows only the app's own ViewModels. */
class StrictFactoryActivity : ComponentActivity() {
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                throw IllegalArgumentException("unknown model class $modelClass")
        }
}

/**
 * Checks that the auto overlay keeps a host for the Activity on screen
 * when another Activity opens and closes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoOverlayActivityTest {

    @get:Rule
    val reset = ToastStackReset()

    @Before
    fun setUp() {
        ToastStackInitializer().create(ApplicationProvider.getApplicationContext())
    }

    private fun launch(): ActivityController<PlainContentActivity> =
        Robolectric.buildActivity(PlainContentActivity::class.java).setup().also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `back from a second Activity sends toasts to the first one again`() {
        val first = launch()
        val firstState = ToastStack.resolveHost(null)
        first.pause()
        val second = launch()
        first.stop()

        // The user presses back: the first Activity resumes before the second one is destroyed.
        second.pause()
        first.restart().start().resume()
        second.stop().destroy()
        idle()

        assertSame(firstState, ToastStack.resolveHost(null))
        assertNotNull(ToastStack.show("hello"))
        assertEquals("hello", firstState!!.toasts.single().message)
        first.pause().stop().destroy()
    }

    @Test
    fun `a splash Activity that finishes does not take the host of the next one`() {
        val splash = launch()
        splash.pause()
        val main = launch()
        val mainState = ToastStack.resolveHost(null)
        splash.stop().destroy()
        idle()

        assertSame(mainState, ToastStack.resolveHost(null))
        assertNotNull(ToastStack.show("hello"))
        main.pause().stop().destroy()
    }

    @Test
    fun `a second resume does not add a second overlay`() {
        val activity = launch()
        activity.pause().resume()
        idle()

        val content = activity.get().findViewById<ViewGroup>(android.R.id.content)
        val overlays = (0 until content.childCount).count {
            content.getChildAt(it).tag == "__toaststack_auto_overlay__"
        }
        assertEquals(1, overlays)
        activity.pause().stop().destroy()
    }

    @Test
    fun `an Activity with no content view resumes and shows toasts`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        idle()

        val content = activity.get().findViewById<ViewGroup>(android.R.id.content)
        assertNotNull(content.findViewWithTag<View>("__toaststack_auto_overlay__"))
        assertNotNull(ToastStack.show("hello"))
        activity.pause().stop().destroy()
    }

    @Test
    fun `setContent after the first resume does not take over the overlay`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        idle()

        controller.get().setContent { }
        controller.pause().resume()
        idle()

        // The app's ComposeView and the overlay, each with its own tree.
        val content = controller.get().findViewById<ViewGroup>(android.R.id.content)
        assertEquals(2, content.childCount)
        assertNotNull(ToastStack.show("hello"))
        controller.pause().stop().destroy()
    }

    @Test
    fun `rotation keeps a visible toast and its handle`() {
        val controller = launch()
        val handle = ToastStack.show("kept", duration = ToastDuration.Indefinite)!!

        controller.recreate()
        idle()

        val state = ToastStack.resolveHost(null)!!
        assertEquals("kept", state.toasts.single().message)
        handle.dismiss()
        assertTrue(state.toasts.isEmpty())
        controller.pause().stop().destroy()
    }

    @Test
    fun `an Activity with its own default ViewModel factory gets an overlay`() {
        val controller = Robolectric.buildActivity(StrictFactoryActivity::class.java).setup()
        idle()

        assertNotNull(ToastStack.show("hello"))
        controller.pause().stop().destroy()
    }

    @Test
    fun `closing the Activity ends its toasts with Programmatic`() {
        val controller = launch()
        var reason: DismissReason? = null
        ToastStack.show("bye", duration = ToastDuration.Indefinite, onDismiss = { reason = it })

        controller.pause().stop().destroy()

        assertEquals(DismissReason.Programmatic, reason)
    }

    @Test
    fun `an Activity left out by the autoOverlay filter gets no overlay`() {
        ToastStack.configure(autoOverlay = { it !is PlainContentActivity })

        val controller = launch()

        val content = controller.get().findViewById<ViewGroup>(android.R.id.content)
        assertNull(content.findViewWithTag<View>(AUTO_OVERLAY_TAG))
        controller.pause().stop().destroy()
    }

    @Test
    fun `a hand-placed default host next to the overlay logs one warning`() {
        ShadowLog.clear()

        val controller = Robolectric.buildActivity(HandPlacedHostActivity::class.java).setup()
        idle()
        controller.pause().resume()
        idle()

        assertEquals(1, ShadowLog.getLogsForTag("ToastStack").count { "same Activity" in it.msg })
        controller.pause().stop().destroy()
    }

    @Test
    fun `a dropped toast writes one warning, and dismiss does not use it up`() {
        ToastStack.warnedNoHost = false
        ShadowLog.clear()

        ToastStack.dismiss("any id", hostTag = "missing")
        assertNull(ToastStack.show("first", hostTag = "missing"))
        assertNull(ToastStack.show("second", hostTag = "missing"))

        val warnings = ShadowLog.getLogsForTag("ToastStack")
        assertEquals(1, warnings.size)
        assertFalse("first" in warnings.single().msg)
    }
}
