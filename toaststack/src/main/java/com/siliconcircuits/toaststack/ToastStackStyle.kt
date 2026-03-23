package com.siliconcircuits.toaststack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Visual appearance overrides for a toast card.
 *
 * Every field is nullable so that callers only specify the properties they
 * want to change. Fields left as null "fall through" to the next layer in
 * the three layer resolution system:
 *
 * 1. **Type defaults** from [ToastStackDefaults.styleForType] provide a
 *    complete baseline for each [ToastType] (all fields non null).
 * 2. **Global style** set on [ToastStackHost] via `globalStyle` overrides
 *    the type defaults for every toast rendered by that host.
 * 3. **Per toast style** set on [ToastData.style] overrides both the
 *    global style and the type defaults for that specific toast only.
 *
 * The merge is performed by [mergeWith], where fields in the override
 * parameter win over fields in the receiver. This means the last layer
 * applied has the highest priority.
 *
 * Example: applying a custom background while keeping the default text color:
 * ```
 * toastState.show(
 *     message = "Custom look",
 *     style = ToastStackStyle(backgroundColor = Color.Magenta)
 * )
 * ```
 * Here only `backgroundColor` is overridden; everything else (text color,
 * icon tint, shape, typography) falls through to the type defaults.
 *
 * @property backgroundColor Fill color behind the entire toast card.
 * @property contentColor Color applied to the message text and used as
 *   a fallback for [iconTint] and [titleColor] when those are null.
 * @property titleColor Color for the optional title text line. When null,
 *   the [contentColor] is used instead.
 * @property iconTint Tint color applied to the leading icon. When null,
 *   the [contentColor] is used instead.
 * @property borderColor Optional stroke color drawn around the card edge.
 *   No border is rendered when this is null.
 * @property borderWidth Thickness of the border stroke. Only meaningful
 *   when [borderColor] is also set. Defaults to 1dp internally if omitted.
 * @property shape The corner shape of the toast card (e.g., rounded corners).
 *   In Compose, shapes define how the edges of a composable are clipped.
 * @property elevation Shadow elevation beneath the toast card. Higher values
 *   create a more prominent shadow, giving the card a "lifted" appearance.
 * @property titleStyle Typography (font family, size, weight) for the
 *   optional title line.
 * @property messageStyle Typography for the main message line.
 */
data class ToastStackStyle(
    val backgroundColor: Color? = null,
    val contentColor: Color? = null,
    val titleColor: Color? = null,
    val iconTint: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Dp? = null,
    val shape: Shape? = null,
    val elevation: Dp? = null,
    val titleStyle: TextStyle? = null,
    val messageStyle: TextStyle? = null
) {
    /**
     * Creates a new [ToastStackStyle] by overlaying [override] on top of
     * this style. For each field, the value from [override] is used if it
     * is non null; otherwise the value from this style is kept.
     *
     * This is how the three layer resolution works in practice:
     * ```
     * val effective = typeDefaults
     *     .mergeWith(globalStyle)    // global overrides type defaults
     *     .mergeWith(perToastStyle)  // per toast overrides everything
     * ```
     *
     * Passing null as the [override] returns this style unchanged, which
     * simplifies call sites that may or may not have an override available.
     *
     * @param override The higher priority style whose non null fields win.
     *   When null, this style is returned as is.
     * @return A merged style combining both layers.
     */
    fun mergeWith(override: ToastStackStyle?): ToastStackStyle {
        if (override == null) return this
        return ToastStackStyle(
            backgroundColor = override.backgroundColor ?: backgroundColor,
            contentColor = override.contentColor ?: contentColor,
            titleColor = override.titleColor ?: titleColor,
            iconTint = override.iconTint ?: iconTint,
            borderColor = override.borderColor ?: borderColor,
            borderWidth = override.borderWidth ?: borderWidth,
            shape = override.shape ?: shape,
            elevation = override.elevation ?: elevation,
            titleStyle = override.titleStyle ?: titleStyle,
            messageStyle = override.messageStyle ?: messageStyle
        )
    }
}
