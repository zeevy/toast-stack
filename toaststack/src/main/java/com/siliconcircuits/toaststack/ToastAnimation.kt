package com.siliconcircuits.toaststack

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * Describes how a toast enters and exits the screen.
 *
 * Each value provides a different visual personality for the toast
 * transition. The animation is resolved into Compose [EnterTransition]
 * and [ExitTransition] objects by [toEnterTransition] and
 * [toExitTransition], using the duration and easing from
 * [ToastAnimationConfig].
 *
 * The slide direction for [Slide] is determined automatically by the
 * toast's [ToastPosition]: top positioned toasts slide down from above,
 * bottom positioned toasts slide up from below, and center positioned
 * toasts fade in without sliding.
 */
@ExperimentalToastStackApi
enum class ToastAnimation {

    /**
     * The toast slides in vertically from the nearest screen edge and
     * fades in simultaneously. On exit, it slides back out and fades.
     * This is the default animation and feels the most natural for
     * notification style content.
     */
    Slide,

    /**
     * The toast fades in from fully transparent to fully opaque.
     * On exit, it fades back to transparent. Use this for a subtle,
     * non distracting appearance that doesn't shift layout.
     */
    Fade,

    /**
     * The toast scales up from 80% size while fading in, creating a
     * "pop in" effect. On exit, it scales down and fades out.
     * Use this when you want the toast to draw more attention.
     */
    ScaleAndFade
}

/**
 * Controls the timing and feel of toast enter/exit animations.
 *
 * Every field has a sensible default so you can create an instance
 * with `ToastAnimationConfig()` and get smooth, balanced transitions
 * out of the box.
 *
 * @property enterDurationMillis How long the enter animation takes in
 *   milliseconds. Shorter values feel snappier, longer values feel smoother.
 * @property exitDurationMillis How long the exit animation takes in
 *   milliseconds. Usually slightly shorter than enter for a crisp feel.
 * @property enterEasing The acceleration curve for the enter animation.
 *   [EaseOut] starts fast and decelerates, which feels natural for
 *   content appearing on screen.
 * @property exitEasing The acceleration curve for the exit animation.
 *   [EaseInOut] provides a balanced start and end for departing content.
 * @property staggerDelayMillis Delay between consecutive toast appearances
 *   when multiple toasts are shown in rapid succession. Creates a cascading
 *   effect rather than all toasts appearing simultaneously.
 */
@ExperimentalToastStackApi
data class ToastAnimationConfig(
    val enterDurationMillis: Int = 300,
    val exitDurationMillis: Int = 250,
    val enterEasing: Easing = EaseOut,
    val exitEasing: Easing = EaseInOut,
    val staggerDelayMillis: Int = 50
)

/**
 * Builds the Compose [EnterTransition] for the given [ToastAnimation] type.
 *
 * The [slideSign] parameter controls the vertical direction for [ToastAnimation.Slide]:
 *  - Negative values make the toast enter from above (for top positioned toasts)
 *  - Positive values make it enter from below (for bottom positioned toasts)
 *  - Zero disables the slide component entirely (for center positioned toasts)
 *
 * All transitions include [expandVertically] so the surrounding stack
 * smoothly makes room for the new toast instead of jumping.
 *
 * @param config Timing and easing parameters for the animation.
 * @param slideSign Direction multiplier for vertical slide. Determined by
 *   the toast's position in [ToastStackHost].
 * @return A combined [EnterTransition] ready to pass to [AnimatedVisibility].
 */
@ExperimentalToastStackApi
internal fun ToastAnimation.toEnterTransition(
    config: ToastAnimationConfig,
    slideSign: Int
): EnterTransition {
    val floatSpec = tween<Float>(
        durationMillis = config.enterDurationMillis,
        easing = config.enterEasing
    )
    val offsetSpec = tween<IntOffset>(
        durationMillis = config.enterDurationMillis,
        easing = config.enterEasing
    )
    val sizeSpec = tween<IntSize>(
        durationMillis = config.enterDurationMillis,
        easing = config.enterEasing
    )

    // expandVertically animates the column height from 0 to full so the
    // surrounding toasts make room smoothly. Only used with Slide because
    // Fade and ScaleAndFade should appear in place without pushing others.
    val expand = expandVertically(animationSpec = sizeSpec)

    return when (this) {
        ToastAnimation.Slide -> {
            slideInVertically(
                initialOffsetY = { height -> slideSign * height },
                animationSpec = offsetSpec
            ) + fadeIn(animationSpec = floatSpec) + expand
        }

        ToastAnimation.Fade -> {
            fadeIn(animationSpec = floatSpec)
        }

        ToastAnimation.ScaleAndFade -> {
            scaleIn(
                initialScale = 0.8f,
                animationSpec = floatSpec
            ) + fadeIn(animationSpec = floatSpec)
        }
    }
}

/**
 * Builds the Compose [ExitTransition] for the given [ToastAnimation] type.
 *
 * Mirrors [toEnterTransition] in reverse. All transitions include
 * [shrinkVertically] so the stack collapses smoothly when a toast
 * is removed, preventing the remaining toasts from jumping into
 * the vacated space.
 *
 * @param config Timing and easing parameters for the animation.
 * @param slideSign Direction multiplier for vertical slide.
 * @return A combined [ExitTransition] ready to pass to [AnimatedVisibility].
 */
@ExperimentalToastStackApi
internal fun ToastAnimation.toExitTransition(
    config: ToastAnimationConfig,
    slideSign: Int
): ExitTransition {
    val floatSpec = tween<Float>(
        durationMillis = config.exitDurationMillis,
        easing = config.exitEasing
    )
    val offsetSpec = tween<IntOffset>(
        durationMillis = config.exitDurationMillis,
        easing = config.exitEasing
    )
    val sizeSpec = tween<IntSize>(
        durationMillis = config.exitDurationMillis,
        easing = config.exitEasing
    )

    // shrinkVertically collapses the space the toast occupied so the
    // surrounding toasts reflow smoothly. Only used with Slide to match
    // the expand on enter. Fade and ScaleAndFade disappear in place.
    val shrink = shrinkVertically(animationSpec = sizeSpec)

    return when (this) {
        ToastAnimation.Slide -> {
            slideOutVertically(
                targetOffsetY = { height -> slideSign * height },
                animationSpec = offsetSpec
            ) + fadeOut(animationSpec = floatSpec) + shrink
        }

        ToastAnimation.Fade -> {
            fadeOut(animationSpec = floatSpec)
        }

        ToastAnimation.ScaleAndFade -> {
            scaleOut(
                targetScale = 0.8f,
                animationSpec = floatSpec
            ) + fadeOut(animationSpec = floatSpec)
        }
    }
}
