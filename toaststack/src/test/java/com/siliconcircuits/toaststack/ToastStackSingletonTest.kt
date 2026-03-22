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

@OptIn(ExperimentalToastStackApi::class, ExperimentalCoroutinesApi::class)
class ToastStackSingletonTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Clean up any registered hosts between tests.
        ToastStack.unregisterHost("host_a")
        ToastStack.unregisterHost("host_b")
        ToastStack.unregisterHost("__test_default__")
    }

    // -- Host registration --

    @Test
    fun `show returns null when no host is registered`() {
        val id = ToastStack.show("Orphan")
        assertNull(id)
    }

    @Test
    fun `show routes to the most recently registered host`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.show("Hello")

        // Should land on host_b (most recent).
        assertTrue(stateA.toasts.isEmpty())
        assertEquals(1, stateB.toasts.size)
    }

    @Test
    fun `show routes to a specific host when tag is provided`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.show("Targeted", hostTag = "host_a")

        assertEquals(1, stateA.toasts.size)
        assertTrue(stateB.toasts.isEmpty())
    }

    @Test
    fun `show returns a valid toast ID on success`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test_default__", state)

        val id = ToastStack.show("Test")
        assertNotNull(id)
        assertEquals(id, state.toasts.first().id)
    }

    // -- Dismiss --

    @Test
    fun `dismiss removes a toast from the resolved host`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test_default__", state)

        val id = ToastStack.show("Removable")!!
        ToastStack.dismiss(id)

        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `dismissAll clears every registered host when no tag given`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        stateA.show("A")
        stateB.show("B")
        ToastStack.dismissAll()

        assertTrue(stateA.toasts.isEmpty())
        assertTrue(stateB.toasts.isEmpty())
    }

    @Test
    fun `dismissAll with tag only clears that host`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        stateA.show("A")
        stateB.show("B")
        ToastStack.dismissAll(hostTag = "host_a")

        assertTrue(stateA.toasts.isEmpty())
        assertEquals(1, stateB.toasts.size)
    }

    // -- Unregister --

    @Test
    fun `unregister falls back to remaining host`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.unregisterHost("host_b")
        ToastStack.show("Fallback")

        assertEquals(1, stateA.toasts.size)
        assertTrue(stateB.toasts.isEmpty())
    }

    @Test
    fun `unregister all hosts causes show to return null`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test_default__", state)
        ToastStack.unregisterHost("__test_default__")

        val id = ToastStack.show("Nowhere")
        assertNull(id)
    }
}
