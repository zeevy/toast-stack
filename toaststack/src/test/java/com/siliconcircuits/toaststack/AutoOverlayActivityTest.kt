package com.siliconcircuits.toaststack

import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** An Activity with plain View content, like most apps before they add Compose. */
class PlainContentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
    }
}

/**
 * Checks that the auto overlay keeps a host for the Activity on screen
 * when another Activity opens and closes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoOverlayActivityTest {

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
