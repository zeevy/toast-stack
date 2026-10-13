package com.siliconcircuits.toaststack

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.util.Collections
import kotlin.concurrent.thread

/** Checks that calls from a background thread are safe and call back on the main thread. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThreadingTest {

    private fun idleFor(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    fun `show and updateProgress from a background thread call back on the main thread`() {
        val state = ToastStackState(maxVisible = 1)
        val main = Thread.currentThread()
        val callbackThreads = Collections.synchronizedList(mutableListOf<Thread>())
        val record: (DismissReason) -> Unit = { callbackThreads += Thread.currentThread() }
        state.show("On screen", duration = ToastDuration.Short, onDismiss = record)

        thread(name = "upload-worker") {
            state.show("Urgent", duration = ToastDuration.Short, priority = ToastPriority.Urgent, onDismiss = record)
            val upload = state.loading("Uploading", onDismiss = record)
            upload.updateProgress(0.5f, "1 of 2")
        }.join()
        idleFor(0)

        // The urgent toast took the slot; the upload waits in the queue with its progress.
        assertEquals("Urgent", state.toasts.single().message)
        idleFor(ToastDuration.Short.millis + 100)
        val upload = state.toasts.single()
        assertEquals(0.5f, upload.progress)
        state.dismiss(upload.id)

        assertEquals(3, callbackThreads.size)
        assertTrue(callbackThreads.all { it === main })
    }

    @Test
    fun `a duplicate shown from a background thread returns a working handle`() {
        val state = ToastStackState(deduplicationWindowMs = 60_000)
        state.show("Saved", duration = ToastDuration.Indefinite)
        lateinit var duplicate: ToastHandle
        thread { duplicate = state.show("Saved", duration = ToastDuration.Indefinite) }.join()
        idleFor(0)
        var reason: DismissReason? = null

        duplicate.onDismiss { reason = it }
        duplicate.dismiss()

        assertTrue(state.toasts.isEmpty())
        assertEquals(DismissReason.Programmatic, reason)
    }

    @Test
    fun `a main thread dismiss right after a background show removes the toast`() {
        val state = ToastStackState()
        lateinit var handle: ToastHandle
        thread { handle = state.loading("Uploading") }.join()

        handle.dismiss() // before the posted add has run
        idleFor(0)

        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun `onDismiss added on main right after a background show still fires`() {
        val state = ToastStackState()
        lateinit var handle: ToastHandle
        thread { handle = state.show("Hi", duration = ToastDuration.Short) }.join()
        var reason: DismissReason? = null

        handle.onDismiss { reason = it } // before the posted add has run
        idleFor(ToastDuration.Short.millis + 100)

        assertEquals(DismissReason.Timeout, reason)
    }

    @Test
    fun `destroy before a posted background show ends its await`() {
        val state = ToastStackState()
        var reason: DismissReason? = null
        thread { state.show("Hi", duration = ToastDuration.Short).onDismiss { reason = it } }.join()

        state.destroy()
        idleFor(ToastDuration.Short.millis + 100)

        assertTrue(state.toasts.isEmpty())
        assertEquals(DismissReason.Programmatic, reason)
    }
}
