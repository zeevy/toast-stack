package com.siliconcircuits.toaststack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class)
class ToastTypeTest {

    @Test
    fun `ToastType has exactly six values`() {
        assertEquals(6, ToastType.entries.size)
    }

    @Test
    fun `ToastType contains all expected names`() {
        val names = ToastType.entries.map { it.name }
        assertEquals(
            listOf("Default", "Success", "Error", "Warning", "Info", "Loading"),
            names
        )
    }

    @Test
    fun `ToastData defaults to Default type`() {
        val toast = ToastData(message = "test")
        assertEquals(ToastType.Default, toast.type)
    }

    @Test
    fun `ToastData preserves explicit type`() {
        ToastType.entries.forEach { type ->
            val toast = ToastData(message = "test", type = type)
            assertEquals(type, toast.type)
        }
    }

    @Test
    fun `ToastData title defaults to null`() {
        assertNull(ToastData(message = "test").title)
    }

    @Test
    fun `ToastData preserves title when set`() {
        val toast = ToastData(message = "Details", title = "Heads up")
        assertEquals("Heads up", toast.title)
        assertEquals("Details", toast.message)
    }

    @Test
    fun `ToastData copy preserves all fields`() {
        val original = ToastData(
            message = "msg",
            title = "ttl",
            type = ToastType.Error,
            duration = ToastDuration.Long,
            position = ToastPosition.BottomEnd,
            showCloseButton = true,
            swipeDismiss = SwipeDismissDirection.Left,
            animation = ToastAnimation.Fade,
            animationConfig = ToastAnimationConfig(enterDurationMillis = 500)
        )
        val copy = original.copy(message = "new msg")
        assertEquals("new msg", copy.message)
        assertEquals("ttl", copy.title)
        assertEquals(ToastType.Error, copy.type)
        assertEquals(ToastAnimation.Fade, copy.animation)
        assertEquals(500, copy.animationConfig?.enterDurationMillis)
    }
}
