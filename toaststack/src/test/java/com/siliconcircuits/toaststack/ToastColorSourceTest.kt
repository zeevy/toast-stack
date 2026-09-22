package com.siliconcircuits.toaststack

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.After
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
        inverseSurface = Color(0xFF000001),
        inverseOnSurface = Color(0xFF000002),
        primaryContainer = Color(0xFF000003),
        onPrimaryContainer = Color(0xFF000004),
        secondaryContainer = Color(0xFF000005),
        onSecondaryContainer = Color(0xFF000006),
        tertiaryContainer = Color(0xFF000007),
        onTertiaryContainer = Color(0xFF000008),
        errorContainer = Color(0xFF000009),
        onErrorContainer = Color(0xFF00000A),
    )

    private val typography = Typography(
        titleSmall = TextStyle(fontSize = 41.sp),
        bodyMedium = TextStyle(fontSize = 42.sp),
    )

    @After
    fun resetGlobals() {
        ToastStack.configure(colorSource = ToastColorSource.AppTheme, theme = null)
    }

    private fun stylesFor(vararg sources: ToastColorSource): Map<ToastColorSource, Map<ToastType, ToastStackStyle>> {
        val styles = mutableMapOf<ToastColorSource, MutableMap<ToastType, ToastStackStyle>>()
        composeTestRule.setContent {
            MaterialTheme(colorScheme = scheme, typography = typography) {
                sources.forEach { source ->
                    val byType = styles.getOrPut(source) { mutableMapOf() }
                    ToastType.entries.forEach { byType[it] = ToastStackDefaults.styleForType(it, source) }
                }
            }
        }
        return styles
    }

    private fun stylesFor(source: ToastColorSource): Map<ToastType, ToastStackStyle> =
        stylesFor(*arrayOf(source)).getValue(source)

    @Test
    fun `AppTheme maps every type to its theme color roles`() {
        val styles = stylesFor(ToastColorSource.AppTheme)
        val expected = mapOf(
            ToastType.Default to (scheme.inverseSurface to scheme.inverseOnSurface),
            ToastType.Loading to (scheme.inverseSurface to scheme.inverseOnSurface),
            ToastType.Success to (scheme.primaryContainer to scheme.onPrimaryContainer),
            ToastType.Info to (scheme.secondaryContainer to scheme.onSecondaryContainer),
            ToastType.Warning to (scheme.tertiaryContainer to scheme.onTertiaryContainer),
            ToastType.Error to (scheme.errorContainer to scheme.onErrorContainer),
        )
        expected.forEach { (type, colors) ->
            val style = styles.getValue(type)
            assertEquals("$type background", colors.first, style.backgroundColor)
            assertEquals("$type content", colors.second, style.contentColor)
            assertEquals("$type title", colors.second, style.titleColor)
            assertEquals("$type icon", colors.second, style.iconTint)
        }
    }

    @Test
    fun `typography comes from the theme in both modes`() {
        val all = stylesFor(*ToastColorSource.entries.toTypedArray())
        all.forEach { (source, styles) ->
            val style = styles.getValue(ToastType.Success)
            assertEquals("$source title", typography.titleSmall, style.titleStyle)
            assertEquals("$source message", typography.bodyMedium, style.messageStyle)
        }
    }

    @Test
    fun `Library keeps the fixed hues regardless of theme`() {
        val styles = stylesFor(ToastColorSource.Library)
        assertEquals(Color(0xFF1B5E20), styles.getValue(ToastType.Success).backgroundColor)
        assertEquals(Color(0xFFC62828), styles.getValue(ToastType.Error).backgroundColor)
        assertEquals(Color(0xFFF9A825), styles.getValue(ToastType.Warning).backgroundColor)
        assertEquals(Color(0xFF1565C0), styles.getValue(ToastType.Info).backgroundColor)
        assertEquals(scheme.inverseSurface, styles.getValue(ToastType.Default).backgroundColor)
        assertEquals(scheme.inverseSurface, styles.getValue(ToastType.Loading).backgroundColor)
    }

    @Test
    fun `configure stores colorSource and theme`() {
        val wrapper: @Composable (@Composable () -> Unit) -> Unit = { content -> content() }
        ToastStack.configure(colorSource = ToastColorSource.Library, theme = wrapper)
        assertEquals(ToastColorSource.Library, ToastStack.colorSource)
        assertEquals(wrapper, ToastStack.theme)
    }

    @Test
    fun `overlay falls back to Library when AppTheme has no theme wrapper`() {
        ToastStack.configure(colorSource = ToastColorSource.AppTheme, theme = null)
        assertEquals(ToastColorSource.Library, ToastStack.overlayColorSource())

        ToastStack.configure(colorSource = ToastColorSource.AppTheme, theme = { content -> content() })
        assertEquals(ToastColorSource.AppTheme, ToastStack.overlayColorSource())

        ToastStack.configure(colorSource = ToastColorSource.Library, theme = null)
        assertEquals(ToastColorSource.Library, ToastStack.overlayColorSource())
    }
}
