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
class ToastStackSingletonTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ToastStack.unregisterHost("host_a")
        ToastStack.unregisterHost("host_b")
        ToastStack.unregisterHost("__test__")
    }

    // -- No host registered --

    @Test
    fun `show returns null when no host is registered`() {
        assertNull(ToastStack.show("Orphan"))
    }

    @Test
    fun `success returns null when no host is registered`() {
        assertNull(ToastStack.success("Orphan"))
    }

    @Test
    fun `error returns null when no host is registered`() {
        assertNull(ToastStack.error("Orphan"))
    }

    @Test
    fun `warning returns null when no host is registered`() {
        assertNull(ToastStack.warning("Orphan"))
    }

    @Test
    fun `info returns null when no host is registered`() {
        assertNull(ToastStack.info("Orphan"))
    }

    // -- Most recent host routing --

    @Test
    fun `show routes to the most recently registered host`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.show("Hello")

        assertTrue(stateA.toasts.isEmpty())
        assertEquals(1, stateB.toasts.size)
    }

    // -- Explicit tag routing --

    @Test
    fun `show routes to specific host when tag is provided`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.show("Targeted", hostTag = "host_a")

        assertEquals(1, stateA.toasts.size)
        assertTrue(stateB.toasts.isEmpty())
    }

    @Test
    fun `show returns valid toast ID on success`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)

        val handle = ToastStack.show("Test")
        assertNotNull(handle)
        assertEquals(handle!!.id, state.toasts.first().id)
    }

    // -- Typed methods --

    @Test
    fun `singleton success sets correct type`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.success("Done")
        assertEquals(ToastType.Success, state.toasts.first().type)
    }

    @Test
    fun `singleton error sets correct type`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.error("Fail")
        assertEquals(ToastType.Error, state.toasts.first().type)
    }

    @Test
    fun `singleton warning sets correct type`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.warning("Caution")
        assertEquals(ToastType.Warning, state.toasts.first().type)
    }

    @Test
    fun `singleton info sets correct type`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.info("Note")
        assertEquals(ToastType.Info, state.toasts.first().type)
    }

    @Test
    fun `singleton typed methods accept title`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.success("msg", title = "Title")
        assertEquals("Title", state.toasts.first().title)
    }

    @Test
    fun `singleton typed methods route to specific host`() {
        val stateA = ToastStackState()
        val stateB = ToastStackState()
        ToastStack.registerHost("host_a", stateA)
        ToastStack.registerHost("host_b", stateB)

        ToastStack.error("Oops", hostTag = "host_a")
        assertEquals(1, stateA.toasts.size)
        assertTrue(stateB.toasts.isEmpty())
    }

    // -- Show with all parameters --

    @Test
    fun `singleton show passes type title and style to state`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)

        ToastStack.show(
            message = "styled",
            title = "Ttl",
            type = ToastType.Warning,
            style = ToastStackStyle(
                backgroundColor = androidx.compose.ui.graphics.Color.Magenta
            )
        )

        val toast = state.toasts.first()
        assertEquals("styled", toast.message)
        assertEquals("Ttl", toast.title)
        assertEquals(ToastType.Warning, toast.type)
        assertNotNull(toast.style)
    }

    // -- Dismiss --

    @Test
    fun `dismiss removes toast from the resolved host`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)

        val handle = ToastStack.show("Removable")!!
        ToastStack.dismiss(handle.id)
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `dismiss with unknown ID does not crash`() {
        val state = ToastStackState()
        ToastStack.registerHost("__test__", state)
        ToastStack.dismiss("nonexistent")
    }

    // -- Dismiss all --

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

    @Test
    fun `dismissAll with no hosts registered does not crash`() {
        ToastStack.unregisterHost("host_a")
        ToastStack.unregisterHost("host_b")
        ToastStack.unregisterHost("__test__")
        ToastStack.dismissAll()
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
        ToastStack.registerHost("__test__", state)
        ToastStack.unregisterHost("__test__")
        assertNull(ToastStack.show("Nowhere"))
    }

    @Test
    fun `register sets hostTag on state`() {
        val state = ToastStackState()
        ToastStack.registerHost("my_tag", state)
        assertEquals("my_tag", state.hostTag)
        ToastStack.unregisterHost("my_tag")
    }
}
