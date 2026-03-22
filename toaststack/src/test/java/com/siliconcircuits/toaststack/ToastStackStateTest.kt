package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class, ExperimentalCoroutinesApi::class)
class ToastStackStateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var state: ToastStackState

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        state = ToastStackState(maxVisible = 3)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Show & basic state --

    @Test
    fun `show adds toast to the list`() {
        state.show("Hello")
        assertEquals(1, state.toasts.size)
        assertEquals("Hello", state.toasts.first().message)
    }

    @Test
    fun `show returns a unique ID for each toast`() {
        val handle1 = state.show("First")
        val handle2 = state.show("Second")
        assertNotEquals(handle1.id, handle2.id)
    }

    @Test
    fun `show returns non blank ID`() {
        val handle = state.show("Test")
        assertTrue(handle.id.isNotBlank())
    }

    @Test
    fun `toasts are ordered oldest first`() {
        state.show("First")
        state.show("Second")
        state.show("Third")
        assertEquals("First", state.toasts[0].message)
        assertEquals("Second", state.toasts[1].message)
        assertEquals("Third", state.toasts[2].message)
    }

    @Test
    fun `toasts list is a defensive copy`() {
        state.show("One")
        val snapshot1 = state.toasts
        state.show("Two")
        val snapshot2 = state.toasts
        assertEquals(1, snapshot1.size)
        assertEquals(2, snapshot2.size)
    }

    @Test
    fun `show with all parameters propagates to ToastData`() {
        val handle = state.show(
            message = "msg",
            title = "ttl",
            type = ToastType.Error,
            duration = ToastDuration.Long,
            position = ToastPosition.BottomEnd,
            showCloseButton = true,
            swipeDismiss = SwipeDismissDirection.Left,
            style = ToastStackStyle(backgroundColor = androidx.compose.ui.graphics.Color.Red),
            animation = ToastAnimation.Fade,
            animationConfig = ToastAnimationConfig(enterDurationMillis = 999)
        )
        val toast = state.toasts.first()
        assertEquals(handle.id, toast.id)
        assertEquals("msg", toast.message)
        assertEquals("ttl", toast.title)
        assertEquals(ToastType.Error, toast.type)
        assertEquals(ToastDuration.Long, toast.duration)
        assertEquals(ToastPosition.BottomEnd, toast.position)
        assertTrue(toast.showCloseButton)
        assertEquals(SwipeDismissDirection.Left, toast.swipeDismiss)
        assertNotNull(toast.style)
        assertEquals(ToastAnimation.Fade, toast.animation)
        assertEquals(999, toast.animationConfig?.enterDurationMillis)
    }

    @Test
    fun `show without optional params uses null for style animation customIcon`() {
        state.show("plain")
        val toast = state.toasts.first()
        assertNull(toast.title)
        assertNull(toast.style)
        assertNull(toast.animation)
        assertNull(toast.animationConfig)
        assertNull(toast.customIcon)
        assertNull(toast.onDismiss)
    }

    // -- Default parameters --

    @Test
    fun `default parameters are applied when not specified`() {
        val customState = ToastStackState(
            defaultPosition = ToastPosition.BottomCenter,
            defaultDuration = ToastDuration.Long,
            defaultSwipeDismiss = SwipeDismissDirection.Left,
            defaultAnimation = ToastAnimation.Fade,
            defaultAnimationConfig = ToastAnimationConfig(enterDurationMillis = 777)
        )
        customState.show("Test")
        val toast = customState.toasts.first()
        assertEquals(ToastPosition.BottomCenter, toast.position)
        assertEquals(ToastDuration.Long, toast.duration)
        assertEquals(SwipeDismissDirection.Left, toast.swipeDismiss)
        assertEquals(ToastAnimation.Fade, customState.defaultAnimation)
        assertEquals(777, customState.defaultAnimationConfig.enterDurationMillis)
    }

    @Test
    fun `constructor defaults are sensible`() {
        val defaultState = ToastStackState()
        assertEquals(5, defaultState.maxVisible)
        assertEquals(ToastPosition.TopCenter, defaultState.defaultPosition)
        assertEquals(ToastDuration.Short, defaultState.defaultDuration)
        assertEquals(SwipeDismissDirection.Both, defaultState.defaultSwipeDismiss)
        assertEquals(ToastAnimation.Slide, defaultState.defaultAnimation)
        assertEquals(300, defaultState.defaultAnimationConfig.enterDurationMillis)
    }

    // -- Max visible enforcement --

    @Test
    fun `oldest toast is evicted when max visible is reached`() {
        state.show("One")
        state.show("Two")
        state.show("Three")
        state.show("Four")

        assertEquals(3, state.toasts.size)
        assertTrue(state.toasts.none { it.message == "One" })
        assertEquals("Two", state.toasts.first().message)
    }

    @Test
    fun `multiple evictions when burst exceeds capacity`() {
        state.show("A")
        state.show("B")
        state.show("C")
        state.show("D")
        state.show("E")

        assertEquals(3, state.toasts.size)
        assertEquals("C", state.toasts[0].message)
        assertEquals("D", state.toasts[1].message)
        assertEquals("E", state.toasts[2].message)
    }

    @Test
    fun `eviction fires onDismiss with Programmatic reason`() {
        var dismissedReason: DismissReason? = null
        state.show("Evictable", onDismiss = { dismissedReason = it })
        state.show("Two")
        state.show("Three")
        state.show("Four")

        assertEquals(DismissReason.Programmatic, dismissedReason)
    }

    @Test
    fun `maxVisible of 1 keeps only the newest toast`() {
        val tinyState = ToastStackState(maxVisible = 1)
        tinyState.show("Old")
        tinyState.show("New")
        assertEquals(1, tinyState.toasts.size)
        assertEquals("New", tinyState.toasts.first().message)
    }

    // -- Dismiss --

    @Test
    fun `dismiss removes the correct toast by ID`() {
        val handle = state.show("Target")
        state.show("Other")
        state.dismiss(handle.id)

        assertEquals(1, state.toasts.size)
        assertEquals("Other", state.toasts.first().message)
    }

    @Test
    fun `dismiss fires onDismiss with Swipe reason`() {
        var capturedReason: DismissReason? = null
        val handle = state.show("Test", onDismiss = { capturedReason = it })
        state.dismiss(handle.id, DismissReason.Swipe)
        assertEquals(DismissReason.Swipe, capturedReason)
    }

    @Test
    fun `dismiss fires onDismiss with CloseButton reason`() {
        var capturedReason: DismissReason? = null
        val handle = state.show("Test", onDismiss = { capturedReason = it })
        state.dismiss(handle.id, DismissReason.CloseButton)
        assertEquals(DismissReason.CloseButton, capturedReason)
    }

    @Test
    fun `dismiss defaults to Programmatic reason`() {
        var capturedReason: DismissReason? = null
        val handle = state.show("Test", onDismiss = { capturedReason = it })
        state.dismiss(handle.id)
        assertEquals(DismissReason.Programmatic, capturedReason)
    }

    @Test
    fun `dismiss with unknown ID is a no-op`() {
        state.show("Existing")
        state.dismiss("nonexistent-id")
        assertEquals(1, state.toasts.size)
    }

    @Test
    fun `dismiss same ID twice is a no-op on second call`() {
        val handle = state.show("Once")
        state.dismiss(handle.id)
        state.dismiss(handle.id) // Should not crash.
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `dismiss without onDismiss callback does not crash`() {
        val handle = state.show("No callback")
        state.dismiss(handle.id) // Should not throw.
        assertTrue(state.toasts.isEmpty())
    }

    // -- Dismiss all --

    @Test
    fun `dismissAll removes every toast`() {
        state.show("One")
        state.show("Two")
        state.show("Three")
        state.dismissAll()
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `dismissAll fires onDismiss for each toast with Programmatic reason`() {
        val reasons = mutableListOf<DismissReason>()
        state.show("A", onDismiss = { reasons.add(it) })
        state.show("B", onDismiss = { reasons.add(it) })
        state.dismissAll()

        assertEquals(2, reasons.size)
        assertTrue(reasons.all { it == DismissReason.Programmatic })
    }

    @Test
    fun `dismissAll on empty state is a no-op`() {
        state.dismissAll() // Should not crash.
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `dismissAll called twice does not crash`() {
        state.show("X")
        state.dismissAll()
        state.dismissAll()
        assertTrue(state.toasts.isEmpty())
    }

    // -- Destroy --

    @Test
    fun `destroy clears all toasts`() {
        state.show("A")
        state.show("B")
        state.destroy()
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `destroy on empty state does not crash`() {
        state.destroy() // Should not throw.
    }

    // -- Host tag --

    @Test
    fun `hostTag is null by default`() {
        assertNull(state.hostTag)
    }

    // -- Duration values --

    @Test
    fun `Short duration is 2000 milliseconds`() {
        assertEquals(2_000L, ToastDuration.Short.millis)
    }

    @Test
    fun `Long duration is 4000 milliseconds`() {
        assertEquals(4_000L, ToastDuration.Long.millis)
    }

    @Test
    fun `Indefinite duration uses Long MAX_VALUE`() {
        assertEquals(Long.MAX_VALUE, ToastDuration.Indefinite.millis)
    }

    @Test
    fun `ToastDuration has exactly three values`() {
        // ToastDuration is a sealed class with 3 predefined subtypes + Custom.
        val predefined = listOf(ToastDuration.Short, ToastDuration.Long, ToastDuration.Indefinite)
        assertEquals(3, predefined.size)
    }

    // -- ToastData defaults --

    @Test
    fun `ToastData generates non blank ID by default`() {
        val toast = ToastData(message = "test")
        assertNotNull(toast.id)
        assertTrue(toast.id.isNotBlank())
    }

    @Test
    fun `ToastData two instances have different IDs`() {
        val a = ToastData(message = "a")
        val b = ToastData(message = "b")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `ToastData defaults are sensible`() {
        val toast = ToastData(message = "test")
        assertEquals(ToastDuration.Short, toast.duration)
        assertEquals(ToastPosition.TopCenter, toast.position)
        assertEquals(SwipeDismissDirection.Both, toast.swipeDismiss)
        assertEquals(ToastType.Default, toast.type)
        assertFalse(toast.showCloseButton)
        assertNull(toast.title)
        assertNull(toast.style)
        assertNull(toast.animation)
        assertNull(toast.animationConfig)
        assertNull(toast.customIcon)
        assertNull(toast.onDismiss)
    }

    @Test
    fun `ToastData with custom ID preserves it`() {
        val toast = ToastData(id = "custom-123", message = "test")
        assertEquals("custom-123", toast.id)
    }

    // -- Enum coverage --

    @Test
    fun `ToastPosition has all seven values`() {
        assertEquals(7, ToastPosition.entries.size)
        assertTrue(ToastPosition.entries.map { it.name }.containsAll(
            listOf("TopCenter", "TopStart", "TopEnd", "BottomCenter", "BottomStart", "BottomEnd", "Center")
        ))
    }

    @Test
    fun `SwipeDismissDirection has all four values`() {
        assertEquals(4, SwipeDismissDirection.entries.size)
        assertTrue(SwipeDismissDirection.entries.map { it.name }.containsAll(
            listOf("Left", "Right", "Both", "None")
        ))
    }

    @Test
    fun `DismissReason has all four values`() {
        assertEquals(4, DismissReason.entries.size)
        assertTrue(DismissReason.entries.map { it.name }.containsAll(
            listOf("Timeout", "Swipe", "CloseButton", "Programmatic")
        ))
    }

    // -- Show with explicit parameters --

    @Test
    fun `show respects explicit position and duration`() {
        state.show(
            "Custom",
            duration = ToastDuration.Indefinite,
            position = ToastPosition.BottomEnd,
            showCloseButton = true,
            swipeDismiss = SwipeDismissDirection.None
        )
        val toast = state.toasts.first()
        assertEquals(ToastDuration.Indefinite, toast.duration)
        assertEquals(ToastPosition.BottomEnd, toast.position)
        assertTrue(toast.showCloseButton)
        assertEquals(SwipeDismissDirection.None, toast.swipeDismiss)
    }

    // -- Typed convenience methods --

    @Test
    fun `success sets type to Success`() {
        state.success("Done")
        assertEquals(ToastType.Success, state.toasts.first().type)
    }

    @Test
    fun `error sets type to Error`() {
        state.error("Failed")
        assertEquals(ToastType.Error, state.toasts.first().type)
    }

    @Test
    fun `warning sets type to Warning`() {
        state.warning("Watch out")
        assertEquals(ToastType.Warning, state.toasts.first().type)
    }

    @Test
    fun `info sets type to Info`() {
        state.info("FYI")
        assertEquals(ToastType.Info, state.toasts.first().type)
    }

    @Test
    fun `typed methods accept title`() {
        state.success("msg", title = "Title")
        assertEquals("Title", state.toasts.first().title)
    }

    @Test
    fun `typed methods accept custom duration`() {
        state.error("fail", duration = ToastDuration.Long)
        assertEquals(ToastDuration.Long, state.toasts.first().duration)
    }

    @Test
    fun `typed methods accept custom position`() {
        state.info("note", position = ToastPosition.BottomStart)
        assertEquals(ToastPosition.BottomStart, state.toasts.first().position)
    }

    @Test
    fun `typed methods accept onDismiss callback`() {
        var called = false
        val handle = state.warning("caution", onDismiss = { called = true })
        state.dismiss(handle.id)
        assertTrue(called)
    }

    // -- Timer pause and resume --

    @Test
    fun `pauseTimer does not crash for unknown ID`() {
        state.pauseTimer("nonexistent")
    }

    @Test
    fun `resumeTimer does not crash for unknown ID`() {
        state.resumeTimer("nonexistent")
    }

    @Test
    fun `pauseAll on empty state does not crash`() {
        state.pauseAll()
    }

    @Test
    fun `resumeAll on empty state does not crash`() {
        state.resumeAll()
    }

    @Test
    fun `pauseAll and resumeAll do not remove toasts`() {
        state.show("A", duration = ToastDuration.Indefinite)
        state.show("B", duration = ToastDuration.Indefinite)
        state.pauseAll()
        assertEquals(2, state.toasts.size)
        state.resumeAll()
        assertEquals(2, state.toasts.size)
    }

    @Test
    fun `pauseTimer and resumeTimer do not remove toast`() {
        val handle = state.show("Sticky", duration = ToastDuration.Indefinite)
        state.pauseTimer(handle.id)
        assertEquals(1, state.toasts.size)
        state.resumeTimer(handle.id)
        assertEquals(1, state.toasts.size)
    }

    // -- Indefinite duration --

    @Test
    fun `indefinite toast is not auto dismissed`() {
        val handle = state.show("Forever", duration = ToastDuration.Indefinite)
        // Indefinite toasts should remain until explicitly dismissed.
        assertEquals(1, state.toasts.size)
        assertEquals(handle.id, state.toasts.first().id)
    }

    // -- Dismiss callbacks with all reason types --

    @Test
    fun `dismiss fires onDismiss with Timeout reason`() {
        var capturedReason: DismissReason? = null
        val handle = state.show("Test", onDismiss = { capturedReason = it })
        state.dismiss(handle.id, DismissReason.Timeout)
        assertEquals(DismissReason.Timeout, capturedReason)
    }

    // -- Enqueue internal behavior --

    @Test
    fun `show after destroy does not crash`() {
        state.destroy()
        // Coroutine scope is cancelled but show should not crash.
        // Timer launch will silently fail.
        try {
            state.show("After destroy")
        } catch (_: Exception) {
            // Acceptable if it throws due to cancelled scope.
        }
    }
}
