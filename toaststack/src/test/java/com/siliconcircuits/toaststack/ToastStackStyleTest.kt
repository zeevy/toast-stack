package com.siliconcircuits.toaststack

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalToastStackApi::class)
class ToastStackStyleTest {

    @Test
    fun `mergeWith returns base when override is null`() {
        val base = ToastStackStyle(backgroundColor = Color.Red)
        val result = base.mergeWith(null)
        assertEquals(Color.Red, result.backgroundColor)
    }

    @Test
    fun `mergeWith applies non null override fields`() {
        val base = ToastStackStyle(
            backgroundColor = Color.Red,
            contentColor = Color.White
        )
        val override = ToastStackStyle(backgroundColor = Color.Blue)
        val result = base.mergeWith(override)

        // Override wins for backgroundColor.
        assertEquals(Color.Blue, result.backgroundColor)
        // Base fills in contentColor since override is null.
        assertEquals(Color.White, result.contentColor)
    }

    @Test
    fun `mergeWith leaves fields null when both are null`() {
        val base = ToastStackStyle()
        val override = ToastStackStyle()
        val result = base.mergeWith(override)
        assertNull(result.backgroundColor)
        assertNull(result.borderColor)
    }

    @Test
    fun `mergeWith overrides shape and elevation`() {
        val base = ToastStackStyle(
            shape = RoundedCornerShape(4.dp),
            elevation = 2.dp
        )
        val override = ToastStackStyle(
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        )
        val result = base.mergeWith(override)
        assertEquals(RoundedCornerShape(16.dp), result.shape)
        assertEquals(8.dp, result.elevation)
    }

    @Test
    fun `three layer merge produces correct priority order`() {
        // Simulates: type defaults -> global style -> per toast style.
        val typeDefaults = ToastStackStyle(
            backgroundColor = Color.Gray,
            contentColor = Color.Black,
            iconTint = Color.DarkGray
        )
        val globalOverride = ToastStackStyle(
            backgroundColor = Color.White
        )
        val perToastOverride = ToastStackStyle(
            iconTint = Color.Red
        )
        val result = typeDefaults.mergeWith(globalOverride).mergeWith(perToastOverride)

        // Per toast wins for iconTint.
        assertEquals(Color.Red, result.iconTint)
        // Global wins for backgroundColor.
        assertEquals(Color.White, result.backgroundColor)
        // Type default fills in contentColor (no overrides set it).
        assertEquals(Color.Black, result.contentColor)
    }
}
