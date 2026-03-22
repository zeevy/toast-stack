package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class, ExperimentalCoroutinesApi::class)
class TypedConvenienceMethodsTest {

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

    // -- ToastStackState typed methods --

    @Test
    fun `success sets ToastType to Success`() {
        state.success("Done")
        assertEquals(ToastType.Success, state.toasts.first().type)
    }

    @Test
    fun `error sets ToastType to Error`() {
        state.error("Failed")
        assertEquals(ToastType.Error, state.toasts.first().type)
    }

    @Test
    fun `warning sets ToastType to Warning`() {
        state.warning("Careful")
        assertEquals(ToastType.Warning, state.toasts.first().type)
    }

    @Test
    fun `info sets ToastType to Info`() {
        state.info("FYI")
        assertEquals(ToastType.Info, state.toasts.first().type)
    }

    @Test
    fun `typed methods accept optional title`() {
        state.success("Saved", title = "Success")
        val toast = state.toasts.first()
        assertEquals("Success", toast.title)
        assertEquals("Saved", toast.message)
    }

    @Test
    fun `typed methods return a valid toast ID`() {
        val id = state.error("Oops")
        assertNotNull(id)
        assertEquals(id, state.toasts.first().id)
    }

    // -- ToastStack singleton typed methods --

    @Test
    fun `singleton success routes to registered host`() {
        ToastStack.registerHost("test", state)
        val id = ToastStack.success("Uploaded")
        assertNotNull(id)
        assertEquals(ToastType.Success, state.toasts.first().type)
        ToastStack.unregisterHost("test")
    }

    @Test
    fun `singleton error routes to registered host`() {
        ToastStack.registerHost("test", state)
        val id = ToastStack.error("Crash")
        assertNotNull(id)
        assertEquals(ToastType.Error, state.toasts.first().type)
        ToastStack.unregisterHost("test")
    }

    @Test
    fun `singleton warning routes to registered host`() {
        ToastStack.registerHost("test", state)
        val id = ToastStack.warning("Low battery")
        assertNotNull(id)
        assertEquals(ToastType.Warning, state.toasts.first().type)
        ToastStack.unregisterHost("test")
    }

    @Test
    fun `singleton info routes to registered host`() {
        ToastStack.registerHost("test", state)
        val id = ToastStack.info("Update available")
        assertNotNull(id)
        assertEquals(ToastType.Info, state.toasts.first().type)
        ToastStack.unregisterHost("test")
    }

    @Test
    fun `singleton typed methods return null when no host registered`() {
        // Ensure clean state.
        ToastStack.unregisterHost("test")
        assertEquals(null, ToastStack.success("Nope"))
        assertEquals(null, ToastStack.error("Nope"))
        assertEquals(null, ToastStack.warning("Nope"))
        assertEquals(null, ToastStack.info("Nope"))
    }

    // -- show() with type parameter --

    @Test
    fun `show with explicit type sets it correctly`() {
        state.show("msg", type = ToastType.Warning)
        assertEquals(ToastType.Warning, state.toasts.first().type)
    }

    @Test
    fun `show defaults to Default type when not specified`() {
        state.show("plain")
        assertEquals(ToastType.Default, state.toasts.first().type)
    }
}
