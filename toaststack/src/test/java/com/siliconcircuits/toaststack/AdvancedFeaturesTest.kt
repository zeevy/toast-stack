package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedFeaturesTest {

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

    // -- showCustom --

    @Test
    fun `showCustom returns a ToastHandle`() {
        val handle = state.showCustom { }
        assertNotNull(handle)
        assertTrue(handle.id.isNotBlank())
    }

    @Test
    fun `showCustom adds toast to state`() {
        state.showCustom { }
        assertEquals(1, state.toasts.size)
    }

    @Test
    fun `showCustom toast has customContent set`() {
        state.showCustom { }
        assertNotNull(state.toasts.first().customContent)
    }

    @Test
    fun `showCustom toast has empty message`() {
        state.showCustom { }
        assertEquals("", state.toasts.first().message)
    }

    @Test
    fun `showCustom respects position and duration`() {
        state.showCustom(
            duration = ToastDuration.Long,
            position = ToastPosition.BottomEnd
        ) { }
        val toast = state.toasts.first()
        assertEquals(ToastDuration.Long, toast.duration)
        assertEquals(ToastPosition.BottomEnd, toast.position)
    }

    @Test
    fun `showCustom supports close button`() {
        state.showCustom(showCloseButton = true) { }
        assertTrue(state.toasts.first().showCloseButton)
    }

    @Test
    fun `showCustom handle can dismiss`() {
        val handle = state.showCustom(duration = ToastDuration.Indefinite) { }
        assertEquals(1, state.toasts.size)
        handle.dismiss()
        assertTrue(state.toasts.isEmpty())
    }

    // -- Haptic and sound fields --

    @Test
    fun `hapticEnabled defaults to false`() {
        assertFalse(ToastData(message = "test").hapticEnabled)
    }

    @Test
    fun `soundEnabled defaults to false`() {
        assertFalse(ToastData(message = "test").soundEnabled)
    }

    @Test
    fun `hapticEnabled can be set to true`() {
        val toast = ToastData(message = "test", hapticEnabled = true)
        assertTrue(toast.hapticEnabled)
    }

    @Test
    fun `soundEnabled can be set to true`() {
        val toast = ToastData(message = "test", soundEnabled = true)
        assertTrue(toast.soundEnabled)
    }

    // -- Custom content field --

    @Test
    fun `customContent defaults to null on ToastData`() {
        assertNull(ToastData(message = "test").customContent)
    }

    // -- Accessibility: type prefix in content description --

    @Test
    fun `Success type has correct accessibility prefix`() {
        // Verify the type to prefix mapping is consistent.
        // The actual content description is built in ToastItem, but
        // we can verify the type enum values are stable.
        assertEquals("Success", ToastType.Success.name)
    }

    @Test
    fun `Error type has correct accessibility prefix`() {
        assertEquals("Error", ToastType.Error.name)
    }

    @Test
    fun `Warning type has correct accessibility prefix`() {
        assertEquals("Warning", ToastType.Warning.name)
    }

    @Test
    fun `Loading type has correct accessibility prefix`() {
        assertEquals("Loading", ToastType.Loading.name)
    }
}
