package com.siliconcircuits.toaststack

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that type default colors come from the surrounding MaterialTheme
 * in AppTheme mode and from the fixed library hues in Library mode.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToastColorSourceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scheme = lightColorScheme(
        primaryContainer = Color(0xFF112233),
        onPrimaryContainer = Color(0xFF445566),
        errorContainer = Color(0xFF778899),
        onErrorContainer = Color(0xFFAABBCC),
    )

    @Test
    fun `AppTheme takes typed colors from the theme color roles`() {
        lateinit var success: ToastStackStyle
        lateinit var error: ToastStackStyle
        composeTestRule.setContent {
            MaterialTheme(colorScheme = scheme) {
                success = ToastStackDefaults.styleForType(ToastType.Success, ToastColorSource.AppTheme)
                error = ToastStackDefaults.styleForType(ToastType.Error, ToastColorSource.AppTheme)
            }
        }
        assertEquals(scheme.primaryContainer, success.backgroundColor)
        assertEquals(scheme.onPrimaryContainer, success.contentColor)
        assertEquals(scheme.errorContainer, error.backgroundColor)
        assertEquals(scheme.onErrorContainer, error.contentColor)
    }

    @Test
    fun `Library keeps the fixed hues regardless of theme`() {
        lateinit var success: ToastStackStyle
        composeTestRule.setContent {
            MaterialTheme(colorScheme = scheme) {
                success = ToastStackDefaults.styleForType(ToastType.Success, ToastColorSource.Library)
            }
        }
        assertEquals(Color(0xFF1B5E20), success.backgroundColor)
        assertEquals(Color.White, success.contentColor)
    }

    @Test
    fun `configure stores colorSource and theme`() {
        val wrapper: @androidx.compose.runtime.Composable (@androidx.compose.runtime.Composable () -> Unit) -> Unit =
            { content -> content() }
        ToastStack.configure(colorSource = ToastColorSource.Library, theme = wrapper)
        assertEquals(ToastColorSource.Library, ToastStack.colorSource)
        assertEquals(wrapper, ToastStack.theme)
        ToastStack.configure(colorSource = ToastColorSource.AppTheme, theme = null)
    }
}
