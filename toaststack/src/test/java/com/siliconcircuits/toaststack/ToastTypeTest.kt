package com.siliconcircuits.toaststack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class)
class ToastTypeTest {

    @Test
    fun `ToastType has all five values`() {
        val types = ToastType.entries
        assertEquals(5, types.size)
    }

    @Test
    fun `ToastData defaults to Default type`() {
        val toast = ToastData(message = "test")
        assertEquals(ToastType.Default, toast.type)
    }

    @Test
    fun `ToastData title defaults to null`() {
        val toast = ToastData(message = "test")
        assertNull(toast.title)
    }

    @Test
    fun `ToastData preserves explicit type`() {
        val toast = ToastData(message = "fail", type = ToastType.Error)
        assertEquals(ToastType.Error, toast.type)
    }

    @Test
    fun `ToastData preserves title when set`() {
        val toast = ToastData(message = "Details here", title = "Heads up")
        assertEquals("Heads up", toast.title)
        assertEquals("Details here", toast.message)
    }
}
