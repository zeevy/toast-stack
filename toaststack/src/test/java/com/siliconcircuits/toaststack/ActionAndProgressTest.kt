package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionAndProgressTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var state: ToastStackState

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        state = ToastStackState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Action buttons via ToastData --

    @Test
    fun `toast with actionLabel stores it`() {
        val toast = ToastData(message = "test", actionLabel = "Undo")
        assertEquals("Undo", toast.actionLabel)
    }

    @Test
    fun `toast without actionLabel defaults to null`() {
        val toast = ToastData(message = "test")
        assertNull(toast.actionLabel)
        assertNull(toast.onAction)
    }

    @Test
    fun `toast with secondary action stores both labels`() {
        val toast = ToastData(
            message = "test",
            actionLabel = "Yes",
            secondaryActionLabel = "No"
        )
        assertEquals("Yes", toast.actionLabel)
        assertEquals("No", toast.secondaryActionLabel)
    }

    // -- Action via ToastHandle chaining --

    @Test
    fun `withAction sets action label on toast`() {
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        handle.withAction("Retry") { }
        assertEquals("Retry", state.toasts.first().actionLabel)
    }

    @Test
    fun `withAction callback is stored`() {
        var called = false
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        handle.withAction("Undo") { called = true }
        state.toasts.first().onAction?.invoke()
        assertTrue(called)
    }

    @Test
    fun `withAction returns same handle for chaining`() {
        val handle = state.show("Test")
        val returned = handle.withAction("Undo") { }
        assertEquals(handle, returned)
    }

    @Test
    fun `withAction on dismissed toast is a no-op`() {
        val handle = state.show("Test")
        handle.dismiss()
        handle.withAction("Undo") { } // Should not crash.
    }

    // -- Loading toasts --

    @Test
    fun `loading sets type to Loading`() {
        state.loading("Processing...")
        assertEquals(ToastType.Loading, state.toasts.first().type)
    }

    @Test
    fun `loading defaults to Indefinite duration`() {
        state.loading("Processing...")
        assertTrue(state.toasts.first().duration is ToastDuration.Indefinite)
    }

    @Test
    fun `loading returns a ToastHandle`() {
        val handle = state.loading("Processing...")
        assertNotNull(handle)
        assertTrue(handle.id.isNotBlank())
    }

    @Test
    fun `loading accepts title`() {
        state.loading("Please wait", title = "Uploading")
        assertEquals("Uploading", state.toasts.first().title)
    }

    // -- Progress via ToastHandle --

    @Test
    fun `updateProgress sets progress value on toast`() {
        val handle = state.loading("Uploading")
        handle.updateProgress(0.5f)
        assertEquals(0.5f, state.toasts.first().progress)
    }

    @Test
    fun `updateProgress clamps to 0 to 1 range`() {
        val handle = state.loading("Uploading")
        handle.updateProgress(-0.5f)
        assertEquals(0f, state.toasts.first().progress)
        handle.updateProgress(1.5f)
        assertEquals(1f, state.toasts.first().progress)
    }

    @Test
    fun `updateProgress with label sets both`() {
        val handle = state.loading("Uploading")
        handle.updateProgress(0.3f, "3 of 10 files")
        assertEquals(0.3f, state.toasts.first().progress)
        assertEquals("3 of 10 files", state.toasts.first().progressLabel)
    }

    @Test
    fun `updateProgress on dismissed toast is a no-op`() {
        val handle = state.loading("Uploading")
        handle.dismiss()
        handle.updateProgress(0.5f) // Should not crash.
    }

    // -- Progress via ToastData --

    @Test
    fun `ToastData progress defaults to null`() {
        assertNull(ToastData(message = "test").progress)
    }

    @Test
    fun `ToastData progressLabel defaults to null`() {
        assertNull(ToastData(message = "test").progressLabel)
    }

    // -- Loading on singleton --

    @Test
    fun `singleton loading returns handle`() {
        ToastStack.registerHost("test", state)
        val handle = ToastStack.loading("Processing...")
        assertNotNull(handle)
        assertEquals(ToastType.Loading, state.toasts.first().type)
        ToastStack.unregisterHost("test")
    }

    @Test
    fun `singleton loading returns null without host`() {
        assertNull(ToastStack.loading("Orphan"))
    }

    // -- DismissReason.Action --

    @Test
    fun `DismissReason contains Action`() {
        assertTrue(DismissReason.entries.contains(DismissReason.Action))
    }

    // -- ToastPriority --

    @Test
    fun `ToastPriority has four values`() {
        assertEquals(4, ToastPriority.entries.size)
    }

    @Test
    fun `ToastPriority contains all expected names`() {
        val names = ToastPriority.entries.map { it.name }
        assertEquals(listOf("Low", "Normal", "High", "Urgent"), names)
    }

    @Test
    fun `ToastData priority defaults to Normal`() {
        assertEquals(ToastPriority.Normal, ToastData(message = "test").priority)
    }

    @Test
    fun `ToastData preserves explicit priority`() {
        val toast = ToastData(message = "test", priority = ToastPriority.Urgent)
        assertEquals(ToastPriority.Urgent, toast.priority)
    }

    // -- onShow callback --

    @Test
    fun `ToastData onShow defaults to null`() {
        assertNull(ToastData(message = "test").onShow)
    }

    @Test
    fun `onShow chaining sets callback on toast`() {
        var called = false
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        handle.onShow { called = true }
        // Invoke manually since we're not in a Compose context.
        state.toasts.first().onShow?.invoke()
        assertTrue(called)
    }

    @Test
    fun `onShow returns same handle for chaining`() {
        val handle = state.show("Test")
        val returned = handle.onShow { }
        assertEquals(handle, returned)
    }
}
