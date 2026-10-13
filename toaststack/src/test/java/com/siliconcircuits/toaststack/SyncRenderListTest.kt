package com.siliconcircuits.toaststack

import org.junit.Assert.assertEquals
import org.junit.Test

/** The host's render list follows the state's toasts, and keeps leaving ones for their exit. */
class SyncRenderListTest {

    private val a = ToastData(message = "A")
    private val b = ToastData(message = "B")
    private val renderList = mutableListOf<ToastData>()
    private val visibility = mutableMapOf<String, Boolean>()

    @Test
    fun `a new toast is added invisible, so its enter animation plays`() {
        syncRenderList(listOf(a), renderList, visibility, shownIds = emptySet())

        assertEquals(listOf(a), renderList)
        assertEquals(false, visibility[a.id])
    }

    @Test
    fun `a toast an earlier host showed is added visible`() {
        syncRenderList(listOf(a), renderList, visibility, shownIds = setOf(a.id))

        assertEquals(true, visibility[a.id])
    }

    @Test
    fun `an updated toast replaces its old copy`() {
        syncRenderList(listOf(a), renderList, visibility, emptySet())
        val updated = a.copy(progress = 0.5f)

        syncRenderList(listOf(updated), renderList, visibility, emptySet())

        assertEquals(listOf(updated), renderList)
    }

    @Test
    fun `a removed toast stays for its exit and turns invisible`() {
        syncRenderList(listOf(a, b), renderList, visibility, emptySet())
        visibility[a.id] = true

        syncRenderList(listOf(b), renderList, visibility, emptySet())

        assertEquals(listOf(a, b), renderList)
        assertEquals(false, visibility[a.id])
    }
}
