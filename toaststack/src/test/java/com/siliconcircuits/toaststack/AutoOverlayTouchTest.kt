package com.siliconcircuits.toaststack

import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Checks that the auto overlay takes only the touches that land on a toast.
 * The compose rule swaps the window recomposer, which hides lifecycle owner
 * problems, so overlay setup checks do not belong in this class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoOverlayTouchTest {

    // Finds the overlay's Compose tree for the toast node lookups.
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        ToastStackInitializer().create(ApplicationProvider.getApplicationContext())
    }

    private fun launch(): ActivityController<PlainContentActivity> =
        Robolectric.buildActivity(PlainContentActivity::class.java).setup().also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Sends a tap through the window, the way a finger does. */
    private fun tap(activity: PlainContentActivity, x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, time, action, x, y, 0)
            activity.window.decorView.dispatchTouchEvent(event)
            event.recycle()
        }
        idle() // A View posts its click to the main looper.
    }

    @Test
    fun `a tap on empty overlay space reaches the content below`() {
        val controller = launch()
        val decor = controller.get().window.decorView

        tap(controller.get(), decor.width / 2f, decor.height / 2f)

        assertEquals(1, controller.get().taps)
        controller.pause().stop().destroy()
    }

    @Test
    fun `a tap on a toast does not reach the content below`() {
        val controller = launch()
        ToastStack.show("hello", duration = ToastDuration.Indefinite)
        val bounds = composeTestRule.onNodeWithText("hello").fetchSemanticsNode().boundsInWindow

        tap(controller.get(), bounds.center.x, bounds.center.y)

        assertEquals(0, controller.get().taps)
        ToastStack.dismissAll()
        controller.pause().stop().destroy()
    }
}
