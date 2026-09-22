package com.siliconcircuits.toaststack

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Provides the built in default [ToastStackStyle] for each [ToastType].
 *
 * These defaults serve as the bottom layer in the three layer style
 * resolution system:
 * 1. **Type defaults** (this object) - the baseline colors, shape, and
 *    typography for each toast type.
 * 2. **Global style** - optional overrides passed to [ToastStackHost]
 *    via the `globalStyle` parameter, applied to every toast in that host.
 * 3. **Per toast style** - optional overrides set on an individual
 *    [ToastData.style], taking the highest priority.
 *
 * Which colors a type gets depends on [ToastColorSource]:
 * - [ToastColorSource.AppTheme]: every type maps to Material 3 color roles
 *   from the surrounding [MaterialTheme], so toasts follow the app's
 *   palette, light and dark mode, and dynamic color.
 * - [ToastColorSource.Library]: Default and Loading use the theme's
 *   inverse surface. Success, Error, Warning and Info use fixed hues.
 *
 * Typography always comes from the current [MaterialTheme].
 */
object ToastStackDefaults {

    /**
     * The default corner shape applied to every toast card.
     * 12dp rounded corners give a modern, pill like appearance without
     * being fully circular, matching common Material 3 card patterns.
     */
    val Shape = RoundedCornerShape(12.dp)

    /**
     * The default shadow elevation beneath each toast card.
     * 4dp provides a subtle lift effect that separates the toast from
     * the content behind it without being visually heavy. In Compose,
     * elevation translates to a shadow drawn below the composable.
     */
    val Elevation = 4.dp

    /**
     * Builds and returns a fully populated [ToastStackStyle] for the
     * given [ToastType].
     *
     * This function must be called inside a `@Composable` context because
     * it reads [MaterialTheme.colorScheme] and [MaterialTheme.typography],
     * which are only available during composition (the process where Compose
     * builds or updates the UI tree).
     *
     * Each type returns a complete style with no null fields, so it can
     * serve as a reliable fallback when higher priority layers leave
     * fields unset.
     *
     * @param type The semantic toast type to resolve defaults for.
     * @param colorSource Where the colors come from. Defaults to the value
     *   set in [ToastStack.configure].
     * @return A [ToastStackStyle] with all fields populated for the
     *   given type.
     */
    @Composable
    fun styleForType(
        type: ToastType,
        colorSource: ToastColorSource = ToastStack.colorSource,
    ): ToastStackStyle {
        val colorScheme = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        val (background, content) = when (colorSource) {
            ToastColorSource.AppTheme -> when (type) {
                ToastType.Default, ToastType.Loading ->
                    colorScheme.inverseSurface to colorScheme.inverseOnSurface
                ToastType.Success ->
                    colorScheme.primaryContainer to colorScheme.onPrimaryContainer
                ToastType.Info ->
                    colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
                ToastType.Warning ->
                    colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
                ToastType.Error ->
                    colorScheme.errorContainer to colorScheme.onErrorContainer
            }
            ToastColorSource.Library -> when (type) {
                // Inverse surface adapts to light/dark mode: dark card on a
                // light theme, light card on a dark theme. Loading uses the
                // same colors since the progress indicator carries the emphasis.
                ToastType.Default, ToastType.Loading ->
                    colorScheme.inverseSurface to colorScheme.inverseOnSurface
                // Green darkened from #2E7D32 to meet WCAG AA 4.5:1 with white text.
                ToastType.Success -> Color(0xFF1B5E20) to Color.White
                ToastType.Error -> Color(0xFFC62828) to Color.White
                // Amber with dark text. White text on yellow has poor contrast.
                ToastType.Warning -> Color(0xFFF9A825) to Color(0xFF1B1B1B)
                ToastType.Info -> Color(0xFF1565C0) to Color.White
            }
        }

        return ToastStackStyle(
            backgroundColor = background,
            contentColor = content,
            titleColor = content,
            iconTint = content,
            shape = Shape,
            elevation = Elevation,
            titleStyle = typography.titleSmall,
            messageStyle = typography.bodyMedium
        )
    }
}
