package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Checks that a host which follows the global defaults (the auto overlay)
 * picks up a [ToastStack.configure] call made after it was created.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalDefaultsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var state: ToastStackState

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        state = ToastStackState().apply { followsGlobalDefaults = true }
        ToastStack.registerHost(TAG, state)
    }

    @After
    fun tearDown() {
        ToastStack.unregisterHost(TAG)
        ToastStack.configure(
            defaultDuration = ToastDuration.Short,
            defaultPosition = ToastPosition.TopCenter,
            defaultAnimation = ToastAnimation.Slide
        )
        state.destroy()
        Dispatchers.resetMain()
    }

    @Test
    fun `toast shown after configure uses the new default duration`() {
        ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))
        ToastStack.warning("x")

        testDispatcher.scheduler.advanceTimeBy(7_900)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, state.toasts.size)

        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.runCurrent()
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `global show with no duration uses the configured default`() {
        ToastStack.configure(defaultDuration = ToastDuration.Long, defaultPosition = ToastPosition.BottomCenter)
        ToastStack.show("x")

        val toast = state.toasts.single()
        assertEquals(ToastDuration.Long, toast.duration)
        assertEquals(ToastPosition.BottomCenter, toast.position)
    }

    @Test
    fun `explicit duration wins over the configured default`() {
        ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))
        ToastStack.show("x", duration = ToastDuration.Short)

        assertEquals(ToastDuration.Short, state.toasts.single().duration)
    }

    @Test
    fun `toast already on screen keeps the duration it started with`() {
        ToastStack.info("first")
        ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))

        assertEquals(ToastDuration.Short, state.toasts.single().duration)
        testDispatcher.scheduler.advanceTimeBy(2_100)
        testDispatcher.scheduler.runCurrent()
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `host that does not follow global defaults keeps its own default`() {
        val own = ToastStackState(defaultDuration = ToastDuration.Long)
        ToastStack.registerHost(OWN_TAG, own)
        ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))

        ToastStack.show("x", hostTag = OWN_TAG)

        assertEquals(ToastDuration.Long, own.toasts.single().duration)
        ToastStack.unregisterHost(OWN_TAG)
        own.destroy()
    }

    @Test
    fun `builder uses the configured default duration`() {
        ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))
        buildToast { message = "x" }

        assertEquals(8_000L, state.toasts.single().duration.millis)
    }

    @Test
    fun `toast already on screen keeps the animation it started with`() {
        ToastStack.info("first")
        ToastStack.configure(defaultAnimation = ToastAnimation.Fade)

        assertEquals(ToastAnimation.Slide, state.toasts.single().animation)
    }

    private companion object {
        const val TAG = "__global_defaults_test__"
        const val OWN_TAG = "__own_defaults_test__"
    }
}
