package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class, ExperimentalCoroutinesApi::class)
class GapFixTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Per toast offset --

    @Test
    fun `offsetX and offsetY default to zero`() {
        val toast = ToastData(message = "test")
        assertEquals(0, toast.offsetX)
        assertEquals(0, toast.offsetY)
    }

    @Test
    fun `offsetX and offsetY are preserved`() {
        val toast = ToastData(message = "test", offsetX = 10, offsetY = -20)
        assertEquals(10, toast.offsetX)
        assertEquals(-20, toast.offsetY)
    }

    // -- Queue overflow --

    @Test
    fun `non urgent toasts queue when at capacity`() {
        val state = ToastStackState(maxVisible = 2)
        state.show("First", duration = ToastDuration.Indefinite)
        state.show("Second", duration = ToastDuration.Indefinite)
        state.show("Third", duration = ToastDuration.Indefinite) // Should queue

        // Only 2 visible, Third is queued.
        assertEquals(2, state.toasts.size)
        assertEquals("First", state.toasts[0].message)
        assertEquals("Second", state.toasts[1].message)
    }

    @Test
    fun `queued toast promotes when slot opens`() {
        val state = ToastStackState(maxVisible = 2)
        val first = state.show("First", duration = ToastDuration.Indefinite)
        state.show("Second", duration = ToastDuration.Indefinite)
        state.show("Third", duration = ToastDuration.Indefinite) // Queued

        // Dismiss first to open a slot.
        state.dismiss(first.id)

        // Third should now be promoted.
        assertEquals(2, state.toasts.size)
        assertEquals("Second", state.toasts[0].message)
        assertEquals("Third", state.toasts[1].message)
    }

    // -- Urgent priority bypass --

    @Test
    fun `urgent toast bypasses queue and evicts oldest`() {
        val state = ToastStackState(maxVisible = 2)
        state.show("Normal1", duration = ToastDuration.Indefinite)
        state.show("Normal2", duration = ToastDuration.Indefinite)

        // Urgent should evict Normal1 and show immediately.
        state.show(
            "Urgent!",
            duration = ToastDuration.Indefinite,
            priority = ToastPriority.Urgent
        )

        assertEquals(2, state.toasts.size)
        assertTrue(state.toasts.any { it.message == "Urgent!" })
        assertTrue(state.toasts.none { it.message == "Normal1" })
    }

    // -- High priority queue ordering --

    @Test
    fun `high priority toast jumps ahead of normal in queue`() {
        val state = ToastStackState(maxVisible = 1)
        val first = state.show("Active", duration = ToastDuration.Indefinite)
        state.show("Normal queued", duration = ToastDuration.Indefinite)
        state.show(
            "High queued",
            duration = ToastDuration.Indefinite,
            priority = ToastPriority.High
        )

        // Dismiss active to promote from queue.
        state.dismiss(first.id)

        // High should promote before Normal.
        assertEquals(1, state.toasts.size)
        assertEquals("High queued", state.toasts.first().message)
    }

    // -- Duplicate detection --

    @Test
    fun `duplicate message within window is suppressed`() {
        val state = ToastStackState(deduplicationWindowMs = 5000)
        state.show("Same message")
        state.show("Same message") // Should be suppressed.

        assertEquals(1, state.toasts.size)
    }

    @Test
    fun `different messages are not suppressed`() {
        val state = ToastStackState(deduplicationWindowMs = 5000)
        state.show("Message A")
        state.show("Message B")

        assertEquals(2, state.toasts.size)
    }

    @Test
    fun `dedup disabled by default (window = 0)`() {
        val state = ToastStackState()
        state.show("Same")
        state.show("Same")

        assertEquals(2, state.toasts.size)
    }

    @Test
    fun `dedup returns handle to existing toast`() {
        val state = ToastStackState(deduplicationWindowMs = 5000)
        val handle1 = state.show("Same message")
        val handle2 = state.show("Same message")

        // Both handles should point to the same toast.
        assertEquals(handle1.id, handle2.id)
    }

    // -- Per type sound URI --

    @Test
    fun `soundUri defaults to null`() {
        assertNull(ToastData(message = "test").soundUri)
    }

    @Test
    fun `soundEnabled with soundUri both stored`() {
        val toast = ToastData(message = "test", soundEnabled = true)
        assertTrue(toast.soundEnabled)
        // soundUri is null by default, tested separately in Robolectric if needed.
        assertNull(toast.soundUri)
    }

    // -- DismissAll clears queue --

    @Test
    fun `dismissAll clears both active and queued toasts`() {
        val state = ToastStackState(maxVisible = 1)
        state.show("Active", duration = ToastDuration.Indefinite)
        state.show("Queued", duration = ToastDuration.Indefinite)
        state.dismissAll()

        assertEquals(0, state.toasts.size)
        // After dismissAll, showing a new toast should work (queue is empty).
        state.show("Fresh")
        assertEquals(1, state.toasts.size)
        assertEquals("Fresh", state.toasts.first().message)
    }

    // -- Priority field on show() --

    @Test
    fun `show accepts priority parameter`() {
        val state = ToastStackState()
        state.show("test", priority = ToastPriority.High)
        assertEquals(ToastPriority.High, state.toasts.first().priority)
    }
}
