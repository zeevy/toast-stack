package com.siliconcircuits.toaststack

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ToastStackStyleTest {

    // -- Defaults --

    @Test
    fun `all fields default to null`() {
        val style = ToastStackStyle()
        assertNull(style.backgroundColor)
        assertNull(style.contentColor)
        assertNull(style.titleColor)
        assertNull(style.iconTint)
        assertNull(style.borderColor)
        assertNull(style.borderWidth)
        assertNull(style.shape)
        assertNull(style.elevation)
        assertNull(style.titleStyle)
        assertNull(style.messageStyle)
    }

    // -- Merge with null --

    @Test
    fun `mergeWith null returns base unchanged`() {
        val base = ToastStackStyle(backgroundColor = Color.Red)
        val result = base.mergeWith(null)
        assertEquals(Color.Red, result.backgroundColor)
    }

    // -- Merge basic fields --

    @Test
    fun `mergeWith applies non null override fields`() {
        val base = ToastStackStyle(
            backgroundColor = Color.Red,
            contentColor = Color.White
        )
        val override = ToastStackStyle(backgroundColor = Color.Blue)
        val result = base.mergeWith(override)

        assertEquals(Color.Blue, result.backgroundColor)
        assertEquals(Color.White, result.contentColor)
    }

    @Test
    fun `mergeWith leaves fields null when both are null`() {
        val result = ToastStackStyle().mergeWith(ToastStackStyle())
        assertNull(result.backgroundColor)
        assertNull(result.borderColor)
    }

    // -- Merge shape and elevation --

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

    // -- Merge typography --

    @Test
    fun `mergeWith overrides titleStyle and messageStyle`() {
        val base = ToastStackStyle(
            titleStyle = TextStyle(fontSize = 14.sp),
            messageStyle = TextStyle(fontSize = 12.sp)
        )
        val override = ToastStackStyle(
            titleStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        val result = base.mergeWith(override)
        assertEquals(18.sp, result.titleStyle?.fontSize)
        assertEquals(12.sp, result.messageStyle?.fontSize)
    }

    // -- Merge border --

    @Test
    fun `mergeWith overrides border color and width`() {
        val base = ToastStackStyle(borderColor = Color.Gray, borderWidth = 1.dp)
        val override = ToastStackStyle(borderColor = Color.Red, borderWidth = 3.dp)
        val result = base.mergeWith(override)
        assertEquals(Color.Red, result.borderColor)
        assertEquals(3.dp, result.borderWidth)
    }

    @Test
    fun `mergeWith keeps base border when override is null`() {
        val base = ToastStackStyle(borderColor = Color.Gray, borderWidth = 1.dp)
        val override = ToastStackStyle()
        val result = base.mergeWith(override)
        assertEquals(Color.Gray, result.borderColor)
        assertEquals(1.dp, result.borderWidth)
    }

    // -- Merge icon tint and title color --

    @Test
    fun `mergeWith overrides iconTint and titleColor`() {
        val base = ToastStackStyle(iconTint = Color.White, titleColor = Color.White)
        val override = ToastStackStyle(iconTint = Color.Yellow)
        val result = base.mergeWith(override)
        assertEquals(Color.Yellow, result.iconTint)
        assertEquals(Color.White, result.titleColor)
    }

    // -- Three layer merge (type defaults -> global -> per toast) --

    @Test
    fun `three layer merge produces correct priority order`() {
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

        assertEquals(Color.Red, result.iconTint)
        assertEquals(Color.White, result.backgroundColor)
        assertEquals(Color.Black, result.contentColor)
    }

    @Test
    fun `three layer merge with all null overrides returns base`() {
        val base = ToastStackStyle(
            backgroundColor = Color.Blue,
            contentColor = Color.White
        )
        val result = base.mergeWith(ToastStackStyle()).mergeWith(ToastStackStyle())
        assertEquals(Color.Blue, result.backgroundColor)
        assertEquals(Color.White, result.contentColor)
    }

    // -- ToastStackDefaults constants --

    @Test
    fun `default shape is RoundedCornerShape 12dp`() {
        assertEquals(RoundedCornerShape(12.dp), ToastStackDefaults.Shape)
    }

    @Test
    fun `default elevation is 4dp`() {
        assertEquals(4.dp, ToastStackDefaults.Elevation)
    }
}
