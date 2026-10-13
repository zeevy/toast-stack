package com.siliconcircuits.toaststack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ToastHandleTest {

    @get:Rule
    val reset = ToastStackReset()

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

    // -- ToastHandle basics --

    @Test
    fun `show returns a ToastHandle with valid ID`() {
        val handle = state.show("Test")
        assertNotNull(handle)
        assertTrue(handle.id.isNotBlank())
    }

    @Test
    fun `handle id matches the toast in state`() {
        val handle = state.show("Test")
        assertEquals(handle.id, state.toasts.first().id)
    }

    @Test
    fun `handle dismiss removes the toast`() {
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        assertEquals(1, state.toasts.size)
        handle.dismiss()
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `handle dismiss is safe to call twice`() {
        val handle = state.show("Test")
        handle.dismiss()
        handle.dismiss() // Should not crash.
        assertTrue(state.toasts.isEmpty())
    }

    // -- Chaining: onDismiss --

    @Test
    fun `onDismiss callback fires when toast is dismissed`() {
        var capturedReason: DismissReason? = null
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        handle.onDismiss { capturedReason = it }
        handle.dismiss()
        assertEquals(DismissReason.Programmatic, capturedReason)
    }

    @Test
    fun `onDismiss chains with existing callback`() {
        var originalCalled = false
        var chainedCalled = false
        val handle = state.show(
            "Test",
            duration = ToastDuration.Indefinite,
            onDismiss = { originalCalled = true }
        )
        handle.onDismiss { chainedCalled = true }
        handle.dismiss()
        assertTrue(originalCalled)
        assertTrue(chainedCalled)
    }

    @Test
    fun `onDismiss returns the same handle for chaining`() {
        val handle = state.show("Test")
        val returned = handle.onDismiss { }
        assertEquals(handle, returned)
    }

    @Test
    fun `onDismiss on already dismissed toast is a no-op`() {
        val handle = state.show("Test")
        handle.dismiss()
        // Should not crash even though the toast is gone.
        handle.onDismiss { }
    }

    @Test
    fun `withSecondaryAction adds the second button`() {
        state.show("Deleted", duration = ToastDuration.Indefinite).withSecondaryAction("View") {}

        assertEquals("View", state.toasts.single().secondaryActionLabel)
    }

    // -- Suspend: await --

    @Test
    fun `await returns DismissReason when toast is dismissed`() = runTest {
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        val deferred = async { handle.await() }

        // Toast is still active, await should not have completed yet.
        advanceUntilIdle()

        // Dismiss the toast, which should resume the coroutine.
        state.dismiss(handle.id, DismissReason.Swipe)
        advanceUntilIdle()

        assertEquals(DismissReason.Swipe, deferred.await())
    }

    @Test
    fun `await returns Programmatic when the state is destroyed`() = runTest {
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        val deferred = async { handle.await() }
        runCurrent()

        state.destroy()

        assertEquals(DismissReason.Programmatic, deferred.await())
    }

    @Test
    fun `await auto dismisses toast on coroutine cancellation`() = runTest {
        val handle = state.show("Test", duration = ToastDuration.Indefinite)
        assertEquals(1, state.toasts.size)

        val job = launch { handle.await() }
        advanceUntilIdle()

        // Cancel the coroutine. The toast should be dismissed.
        job.cancel()
        advanceUntilIdle()

        assertTrue(state.toasts.isEmpty())
    }

    // -- showAndAwait on ToastStackState --

    @Test
    fun `showAndAwait suspends until dismissed`() = runTest {
        val deferred = async {
            state.showAndAwait("Test", duration = ToastDuration.Indefinite)
        }
        advanceUntilIdle()

        assertEquals(1, state.toasts.size)
        val toastId = state.toasts.first().id
        state.dismiss(toastId, DismissReason.CloseButton)
        advanceUntilIdle()

        assertEquals(DismissReason.CloseButton, deferred.await())
    }

    // -- Queued toasts: the stack is full, so the toast waits in the queue --

    /** A state with one slot, already taken by an Indefinite toast. */
    private fun fullState(): Pair<ToastStackState, ToastHandle> {
        val full = ToastStackState(maxVisible = 1)
        return full to full.show("On screen", duration = ToastDuration.Indefinite)
    }

    @Test
    fun `await on a queued toast returns after it shows and times out`() = runTest {
        val (full, onScreen) = fullState()
        val queued = full.show("Queued", duration = ToastDuration.Custom(1_000))
        val deferred = async { queued.await() }
        runCurrent()

        onScreen.dismiss()
        advanceUntilIdle()

        assertEquals(DismissReason.Timeout, deferred.await())
    }

    @Test
    fun `dismiss on a queued toast removes it before it shows`() {
        val (full, onScreen) = fullState()
        var reason: DismissReason? = null
        val loading = full.loading("Loading", onDismiss = { reason = it })

        loading.dismiss()
        onScreen.dismiss()

        assertTrue(full.toasts.isEmpty())
        assertEquals(DismissReason.Programmatic, reason)
    }

    @Test
    fun `withAction and updateProgress on a queued toast reach the card when it shows`() {
        val (full, onScreen) = fullState()
        val queued = full.loading("Uploading").withAction("Cancel") {}
        queued.updateProgress(0.5f, "1 of 2")

        onScreen.dismiss()

        val shown = full.toasts.single()
        assertEquals("Cancel", shown.actionLabel)
        assertEquals(0.5f, shown.progress)
        assertEquals("1 of 2", shown.progressLabel)
    }

    @Test
    fun `showAndAwait returns when the stack was full`() = runTest {
        val (full, onScreen) = fullState()
        val deferred = async { full.showAndAwait("Queued", duration = ToastDuration.Indefinite) }
        runCurrent()

        onScreen.dismiss()
        full.dismiss(full.toasts.single().id, DismissReason.Action)

        assertEquals(DismissReason.Action, deferred.await())
    }

    @Test
    fun `dismissAll ends await on a queued toast`() = runTest {
        val (full, _) = fullState()
        val queued = full.show("Queued", duration = ToastDuration.Indefinite)
        val deferred = async { queued.await() }
        runCurrent()

        full.dismissAll()

        assertEquals(DismissReason.Programmatic, deferred.await())
    }

    @Test
    fun `an onDismiss that throws still lets the next queued toast show`() {
        val full = ToastStackState(maxVisible = 1)
        val first = full.show("First", duration = ToastDuration.Indefinite, onDismiss = { error("app bug") })
        full.show("Next", duration = ToastDuration.Indefinite)

        runCatching { first.dismiss() }

        assertEquals("Next", full.toasts.single().message)
    }

    // -- showAndAwait on ToastStack singleton --

    @Test
    fun `singleton showAndAwait returns null when no host`() = runTest {
        val result = ToastStack.showAndAwait("Orphan")
        assertEquals(null, result)
    }

    @Test
    fun `singleton showAndAwait works with registered host`() = runTest {
        ToastStack.registerHost("test", state)
        val deferred = async {
            ToastStack.showAndAwait("Test", duration = ToastDuration.Indefinite)
        }
        advanceUntilIdle()

        val toastId = state.toasts.first().id
        state.dismiss(toastId, DismissReason.Timeout)
        advanceUntilIdle()

        assertEquals(DismissReason.Timeout, deferred.await())
        ToastStack.unregisterHost("test", state)
    }

    // -- Kotlin Duration support --

    @Test
    fun `ToastDuration Short is 2000 millis`() {
        assertEquals(2_000L, ToastDuration.Short.millis)
    }

    @Test
    fun `ToastDuration Long is 4000 millis`() {
        assertEquals(4_000L, ToastDuration.Long.millis)
    }

    @Test
    fun `ToastDuration Indefinite uses Long MAX_VALUE`() {
        assertEquals(Long.MAX_VALUE, ToastDuration.Indefinite.millis)
    }

    @Test
    fun `ToastDuration Custom accepts arbitrary millis`() {
        val duration = ToastDuration.Custom(3500)
        assertEquals(3500L, duration.millis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ToastDuration Custom rejects zero`() {
        ToastDuration.Custom(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ToastDuration Custom rejects negative`() {
        ToastDuration.Custom(-100)
    }

    @Test
    fun `ToastDuration from Kotlin Duration converts correctly`() {
        val duration = ToastDuration(3.seconds)
        assertEquals(3000L, duration.millis)
    }

    @Test
    fun `ToastDuration from 500 milliseconds`() {
        val duration = ToastDuration(500.milliseconds)
        assertEquals(500L, duration.millis)
    }

    @Test
    fun `ToastDuration from infinite Duration maps to Indefinite`() {
        val duration = ToastDuration(kotlin.time.Duration.INFINITE)
        assertTrue(duration is ToastDuration.Indefinite)
    }

    @Test
    fun `show accepts Custom duration`() {
        state.show("Test", duration = ToastDuration.Custom(5000))
        assertEquals(5000L, state.toasts.first().duration.millis)
    }

    @Test
    fun `show accepts Kotlin Duration via factory`() {
        state.show("Test", duration = ToastDuration(1500.milliseconds))
        assertEquals(1500L, state.toasts.first().duration.millis)
    }

    // -- ToastHandle from singleton --

    @Test
    fun `singleton show returns ToastHandle`() {
        ToastStack.registerHost("test", state)
        val handle = ToastStack.show("Test")
        assertNotNull(handle)
        assertEquals(1, state.toasts.size)
        handle!!.dismiss()
        assertTrue(state.toasts.isEmpty())
        ToastStack.unregisterHost("test", state)
    }

    @Test
    fun `singleton show returns null when no host`() {
        ToastStack.unregisterHost("test", state)
        val handle = ToastStack.show("Orphan")
        assertEquals(null, handle)
    }

    @Test
    fun `singleton typed methods return ToastHandle`() {
        ToastStack.registerHost("test", state)
        val s = ToastStack.success("ok")
        val e = ToastStack.error("fail")
        val w = ToastStack.warning("warn")
        val i = ToastStack.info("note")
        assertNotNull(s)
        assertNotNull(e)
        assertNotNull(w)
        assertNotNull(i)
        assertEquals(4, state.toasts.size)
        ToastStack.unregisterHost("test", state)
    }
}
