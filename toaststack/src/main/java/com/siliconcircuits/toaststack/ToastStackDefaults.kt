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
 * Colors for [ToastType.Default] are read from the Material 3 theme
 * at runtime, so they automatically adapt to:
 * - **Light vs dark mode**: the system or app level theme setting
 * - **Dynamic color (Material You)**: wallpaper based color extraction
 *   available on Android 12+ devices
 *
 * The remaining types (Success, Error, Warning, Info) use fixed brand
 * colors that provide strong visual contrast regardless of theme, while
 * still pulling typography from the current [MaterialTheme].
 */
@ExperimentalToastStackApi
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
     * @return A [ToastStackStyle] with all fields populated for the
     *   given type.
     */
    @Composable
    fun styleForType(type: ToastType): ToastStackStyle {
        val colorScheme = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography

        return when (type) {
            // Default uses M3 inverse surface colors which automatically
            // adapt to light/dark mode: dark card on light theme, light
            // card on dark theme.
            ToastType.Default -> ToastStackStyle(
                backgroundColor = colorScheme.inverseSurface,
                contentColor = colorScheme.inverseOnSurface,
                titleColor = colorScheme.inverseOnSurface,
                iconTint = colorScheme.inverseOnSurface,
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )

            // Green (#1B5E20) with white text for a clear "positive" signal.
            // Darkened from #2E7D32 to meet WCAG AA 4.5:1 contrast ratio
            // with white text (now ~5.8:1).
            ToastType.Success -> ToastStackStyle(
                backgroundColor = Color(0xFF1B5E20),
                contentColor = Color.White,
                titleColor = Color.White,
                iconTint = Color.White,
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )

            // Red (#C62828) with white text for an unmistakable "error" signal.
            ToastType.Error -> ToastStackStyle(
                backgroundColor = Color(0xFFC62828),
                contentColor = Color.White,
                titleColor = Color.White,
                iconTint = Color.White,
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )

            // Amber/yellow (#F9A825) with dark text. Dark text is used
            // instead of white because yellow backgrounds have poor contrast
            // with white text, making it hard to read.
            ToastType.Warning -> ToastStackStyle(
                backgroundColor = Color(0xFFF9A825),
                contentColor = Color(0xFF1B1B1B),
                titleColor = Color(0xFF1B1B1B),
                iconTint = Color(0xFF1B1B1B),
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )

            // Blue (#1565C0) with white text for a calm "informational" tone.
            ToastType.Info -> ToastStackStyle(
                backgroundColor = Color(0xFF1565C0),
                contentColor = Color.White,
                titleColor = Color.White,
                iconTint = Color.White,
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )

            // Loading uses the same inverse surface as Default, since
            // the visual emphasis comes from the progress indicator
            // rather than from the card color.
            ToastType.Loading -> ToastStackStyle(
                backgroundColor = colorScheme.inverseSurface,
                contentColor = colorScheme.inverseOnSurface,
                titleColor = colorScheme.inverseOnSurface,
                iconTint = colorScheme.inverseOnSurface,
                shape = Shape,
                elevation = Elevation,
                titleStyle = typography.titleSmall,
                messageStyle = typography.bodyMedium
            )
        }
    }
}
