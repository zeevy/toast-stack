package com.siliconcircuits.toaststack

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToastAnimationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- ToastAnimation enum --

    @Test
    fun `ToastAnimation has three values`() {
        assertEquals(3, ToastAnimation.entries.size)
    }

    @Test
    fun `ToastAnimation contains Slide Fade and ScaleAndFade`() {
        val names = ToastAnimation.entries.map { it.name }
        assert("Slide" in names)
        assert("Fade" in names)
        assert("ScaleAndFade" in names)
    }

    // -- ToastAnimationConfig defaults --

    @Test
    fun `default enter duration is 300ms`() {
        val config = ToastAnimationConfig()
        assertEquals(300, config.enterDurationMillis)
    }

    @Test
    fun `default exit duration is 250ms`() {
        val config = ToastAnimationConfig()
        assertEquals(250, config.exitDurationMillis)
    }

    @Test
    fun `default enter easing is EaseOut`() {
        val config = ToastAnimationConfig()
        assertEquals(EaseOut, config.enterEasing)
    }

    @Test
    fun `default exit easing is EaseInOut`() {
        val config = ToastAnimationConfig()
        assertEquals(EaseInOut, config.exitEasing)
    }

    @Test
    fun `default stagger delay is 50ms`() {
        val config = ToastAnimationConfig()
        assertEquals(50, config.staggerDelayMillis)
    }

    @Test
    fun `custom config preserves all values`() {
        val config = ToastAnimationConfig(
            enterDurationMillis = 500,
            exitDurationMillis = 400,
            staggerDelayMillis = 100
        )
        assertEquals(500, config.enterDurationMillis)
        assertEquals(400, config.exitDurationMillis)
        assertEquals(100, config.staggerDelayMillis)
    }

    // -- Animation field propagation through show() --

    @Test
    fun `show without animation uses null in ToastData`() {
        val state = ToastStackState()
        state.show("test")
        assertNull(state.toasts.first().animation)
        assertNull(state.toasts.first().animationConfig)
    }

    @Test
    fun `show with animation stores it in ToastData`() {
        val state = ToastStackState()
        state.show("test", animation = ToastAnimation.Fade)
        assertEquals(ToastAnimation.Fade, state.toasts.first().animation)
    }

    @Test
    fun `show with animationConfig stores it in ToastData`() {
        val state = ToastStackState()
        val config = ToastAnimationConfig(enterDurationMillis = 600)
        state.show("test", animationConfig = config)
        assertEquals(600, state.toasts.first().animationConfig?.enterDurationMillis)
    }

    // -- ToastStackState defaults --

    @Test
    fun `state default animation is Slide`() {
        val state = ToastStackState()
        assertEquals(ToastAnimation.Slide, state.defaultAnimation)
    }

    @Test
    fun `state accepts custom default animation`() {
        val state = ToastStackState(defaultAnimation = ToastAnimation.ScaleAndFade)
        assertEquals(ToastAnimation.ScaleAndFade, state.defaultAnimation)
    }

    @Test
    fun `state accepts custom default animation config`() {
        val config = ToastAnimationConfig(enterDurationMillis = 1000)
        val state = ToastStackState(defaultAnimationConfig = config)
        assertEquals(1000, state.defaultAnimationConfig.enterDurationMillis)
    }

    // -- ToastData defaults --

    @Test
    fun `ToastData animation defaults to null`() {
        val toast = ToastData(message = "test")
        assertNull(toast.animation)
    }

    @Test
    fun `ToastData animationConfig defaults to null`() {
        val toast = ToastData(message = "test")
        assertNull(toast.animationConfig)
    }

    @Test
    fun `ToastData preserves explicit animation`() {
        val toast = ToastData(
            message = "test",
            animation = ToastAnimation.ScaleAndFade,
            animationConfig = ToastAnimationConfig(exitDurationMillis = 100)
        )
        assertEquals(ToastAnimation.ScaleAndFade, toast.animation)
        assertEquals(100, toast.animationConfig?.exitDurationMillis)
    }
}
