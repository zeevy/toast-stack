package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        val id1 = state.show("First")
        val id2 = state.show("Second")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `toasts are ordered oldest first`() {
        state.show("First")
        state.show("Second")
        state.show("Third")
        assertEquals("First", state.toasts[0].message)
        assertEquals("Third", state.toasts[2].message)
    }

    @Test
    fun `default parameters are applied when not specified`() {
        val customState = ToastStackState(
            defaultPosition = ToastPosition.BottomCenter,
            defaultDuration = ToastDuration.Long,
            defaultSwipeDismiss = SwipeDismissDirection.Left
        )
        customState.show("Test")
        val toast = customState.toasts.first()
        assertEquals(ToastPosition.BottomCenter, toast.position)
        assertEquals(ToastDuration.Long, toast.duration)
        assertEquals(SwipeDismissDirection.Left, toast.swipeDismiss)
    }

    // -- Max visible enforcement --

    @Test
    fun `oldest toast is evicted when max visible is reached`() {
        state.show("One")
        state.show("Two")
        state.show("Three")
        state.show("Four") // Should evict "One"

        assertEquals(3, state.toasts.size)
        assertTrue(state.toasts.none { it.message == "One" })
        assertEquals("Two", state.toasts.first().message)
    }

    @Test
    fun `eviction fires onDismiss with Programmatic reason`() {
        var dismissedReason: DismissReason? = null
        state.show("Evictable", onDismiss = { dismissedReason = it })
        state.show("Two")
        state.show("Three")
        state.show("Four") // Evicts "Evictable"

        assertEquals(DismissReason.Programmatic, dismissedReason)
    }

    // -- Dismiss --

    @Test
    fun `dismiss removes the correct toast by ID`() {
        val id = state.show("Target")
        state.show("Other")
        state.dismiss(id)

        assertEquals(1, state.toasts.size)
        assertEquals("Other", state.toasts.first().message)
    }

    @Test
    fun `dismiss fires onDismiss callback with provided reason`() {
        var capturedReason: DismissReason? = null
        val id = state.show("Test", onDismiss = { capturedReason = it })
        state.dismiss(id, DismissReason.Swipe)

        assertEquals(DismissReason.Swipe, capturedReason)
    }

    @Test
    fun `dismiss with unknown ID is a no-op`() {
        state.show("Existing")
        state.dismiss("nonexistent-id")
        assertEquals(1, state.toasts.size)
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

    // -- Duration defaults --

    @Test
    fun `Short duration is 2 seconds`() {
        assertEquals(2_000L, ToastDuration.Short.millis)
    }

    @Test
    fun `Long duration is 4 seconds`() {
        assertEquals(4_000L, ToastDuration.Long.millis)
    }

    @Test
    fun `Indefinite duration uses Long MAX_VALUE`() {
        assertEquals(Long.MAX_VALUE, ToastDuration.Indefinite.millis)
    }

    // -- Toast data --

    @Test
    fun `ToastData generates non-null ID by default`() {
        val toast = ToastData(message = "test")
        assertNotNull(toast.id)
        assertTrue(toast.id.isNotBlank())
    }

    @Test
    fun `ToastData defaults are sensible`() {
        val toast = ToastData(message = "test")
        assertEquals(ToastDuration.Short, toast.duration)
        assertEquals(ToastPosition.TopCenter, toast.position)
        assertEquals(SwipeDismissDirection.Both, toast.swipeDismiss)
        assertEquals(false, toast.showCloseButton)
        assertNull(toast.onDismiss)
    }

    // -- Position enum coverage --

    @Test
    fun `ToastPosition has all seven values`() {
        val positions = ToastPosition.entries
        assertEquals(7, positions.size)
        assertTrue(positions.contains(ToastPosition.Center))
    }

    // -- Swipe direction enum coverage --

    @Test
    fun `SwipeDismissDirection has all four values`() {
        assertEquals(4, SwipeDismissDirection.entries.size)
    }

    // -- DismissReason enum coverage --

    @Test
    fun `DismissReason has all four values`() {
        val reasons = DismissReason.entries
        assertEquals(4, reasons.size)
        assertTrue(reasons.containsAll(
            listOf(DismissReason.Timeout, DismissReason.Swipe, DismissReason.CloseButton, DismissReason.Programmatic)
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
        assertEquals(true, toast.showCloseButton)
        assertEquals(SwipeDismissDirection.None, toast.swipeDismiss)
    }
}
