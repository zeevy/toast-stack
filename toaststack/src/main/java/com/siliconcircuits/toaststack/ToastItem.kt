package com.siliconcircuits.toaststack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Minimum horizontal distance in density independent pixels (dp) that the user
 * must drag a toast card before it qualifies as a dismiss gesture.
 * Anything less than this snaps the card back to center on release.
 */
private const val SWIPE_THRESHOLD_DP = 100

/**
 * Minimum horizontal fling speed in pixels per second that triggers a dismiss
 * even when the drag distance is below [SWIPE_THRESHOLD_DP]. A fast flick
 * feels intentional, so we honor it regardless of how far the card moved.
 * 800 px/s is roughly a brisk finger swipe on a typical density screen.
 */
private const val VELOCITY_THRESHOLD_PX_PER_SEC = 800f

/**
 * Duration in milliseconds for the settle animation that plays after the user
 * releases a drag. Used both for the "fly off screen" and "snap back to center"
 * transitions so the two feel consistent.
 */
private const val SETTLE_ANIMATION_MILLIS = 200

/**
 * Renders a single toast card with type aware styling, optional title/description
 * layout, a leading icon, and horizontal swipe to dismiss support.
 *
 * The card's colors, shape, icon, and typography are resolved in three layers:
 * 1. Built in defaults from [ToastStackDefaults.styleForType] based on [ToastData.type].
 * 2. Global style overrides passed through [ToastStackHost].
 * 3. Per toast overrides from [ToastData.style].
 * Each layer only fills in fields that haven't been set by a higher priority layer.
 *
 * Accessibility: the card is wrapped in a [LiveRegionMode.Polite] semantic node
 * so screen readers (TalkBack) announce the toast content when it appears.
 *
 * Timer coordination: the auto dismiss clock pauses the moment the user starts
 * dragging and resumes only if the card snaps back (drag below threshold).
 *
 * @param toast The data model describing what to render and how.
 * @param globalStyle Optional host level style overrides applied between type
 *   defaults and per toast overrides.
 * @param onDismiss Callback invoked when the toast should be removed, with the
 *   [DismissReason] indicating what triggered the dismissal.
 * @param onPauseTimer Pauses the auto dismiss countdown while the user is
 *   interacting with the card.
 * @param onResumeTimer Resumes the countdown after the interaction ends
 *   without a dismiss.
 */
@ExperimentalToastStackApi
@Composable
internal fun ToastItem(
    toast: ToastData,
    globalStyle: ToastStackStyle? = null,
    onDismiss: (DismissReason) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val horizontalOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    // Resolve the effective style by layering: type defaults -> global -> per toast.
    val typeDefaults = ToastStackDefaults.styleForType(toast.type)
    val resolvedStyle = typeDefaults.mergeWith(globalStyle).mergeWith(toast.style)

    val swipeModifier = buildSwipeModifier(
        toast = toast,
        horizontalOffset = horizontalOffset,
        swipeThresholdPx = swipeThresholdPx,
        coroutineScope = coroutineScope,
        onDismiss = onDismiss,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer
    )

    // Gradually reduce opacity as the card moves further from center,
    // giving the user visual feedback that releasing will dismiss.
    val dragAlpha = if (swipeThresholdPx > 0f) {
        (1f - (horizontalOffset.value.absoluteValue / (swipeThresholdPx * 1.5f)))
            .coerceIn(0.3f, 1f)
    } else {
        1f
    }

    // Build the accessibility description from title + message so TalkBack
    // reads both when the toast appears.
    val accessibilityLabel = buildString {
        if (toast.title != null) {
            append(toast.title)
            append(". ")
        }
        append(toast.message)
    }

    // Determine the border stroke. Only drawn when the style specifies a border color.
    val borderStroke = if (resolvedStyle.borderColor != null) {
        BorderStroke(resolvedStyle.borderWidth ?: 1.dp, resolvedStyle.borderColor)
    } else {
        null
    }

    // Width strategy depends on the toast's position:
    // - Center aligned (TopCenter, BottomCenter, Center): fill the available width
    //   for a clean edge to edge appearance, similar to a snackbar
    // - Start / End corners: constrained width so the card wraps its content and the
    //   alignment difference between Start and End is visible on screen
    val useFullWidth = toast.position == ToastPosition.TopCenter ||
        toast.position == ToastPosition.BottomCenter ||
        toast.position == ToastPosition.Center
    val widthModifier = if (useFullWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.widthIn(min = 200.dp, max = 320.dp)
    }

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = widthModifier
                .offset { IntOffset(horizontalOffset.value.roundToInt(), 0) }
                .alpha(dragAlpha)
                .then(swipeModifier)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = accessibilityLabel
                },
            shape = resolvedStyle.shape ?: ToastStackDefaults.Shape,
            shadowElevation = resolvedStyle.elevation ?: ToastStackDefaults.Elevation,
            tonalElevation = 2.dp,
            color = resolvedStyle.backgroundColor ?: typeDefaults.backgroundColor!!,
            border = borderStroke
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        end = if (toast.showCloseButton) 4.dp else 12.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon: either a custom composable or the built in icon for the type.
                val iconContent = toast.customIcon
                if (iconContent != null) {
                    iconContent()
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    val typeIcon = iconForType(toast.type)
                    if (typeIcon != null) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = "${toast.type.name} icon",
                            tint = resolvedStyle.iconTint
                                ?: resolvedStyle.contentColor
                                ?: typeDefaults.contentColor!!,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }

                // Text area: title (optional, bold) + message.
                Column(modifier = Modifier.weight(1f)) {
                    if (toast.title != null) {
                        Text(
                            text = toast.title,
                            style = resolvedStyle.titleStyle!!,
                            color = resolvedStyle.titleColor
                                ?: resolvedStyle.contentColor
                                ?: typeDefaults.contentColor!!
                        )
                    }
                    Text(
                        text = toast.message,
                        style = resolvedStyle.messageStyle!!,
                        color = resolvedStyle.contentColor ?: typeDefaults.contentColor!!
                    )
                }

                // Close button, rendered only when the toast opts in.
                if (toast.showCloseButton) {
                    IconButton(
                        onClick = { onDismiss(DismissReason.CloseButton) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss toast",
                            tint = resolvedStyle.contentColor ?: typeDefaults.contentColor!!,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns the default [ImageVector] icon for a [ToastType], or null for
 * [ToastType.Default] which renders without a leading icon.
 *
 * Uses Material Icons from the `material-icons-extended` set bundled
 * with `material3`. Each icon is chosen to match the semantic meaning:
 * - [ToastType.Success] - a check mark inside a circle
 * - [ToastType.Error] - the Close/X icon repurposed as an error indicator
 * - [ToastType.Warning] - a triangle with an exclamation mark
 * - [ToastType.Info] - the letter "i" inside a circle
 */
@ExperimentalToastStackApi
private fun iconForType(type: ToastType): ImageVector? {
    return when (type) {
        ToastType.Default -> null
        ToastType.Success -> Icons.Default.CheckCircle
        ToastType.Error -> Icons.Default.Close
        ToastType.Warning -> Icons.Default.Warning
        ToastType.Info -> Icons.Default.Info
    }
}

/**
 * Constructs the swipe gesture [Modifier] based on the toast's permitted
 * [SwipeDismissDirection].
 *
 * When swipe is set to [SwipeDismissDirection.None], this returns [Modifier]
 * (the identity modifier) so no gesture detector is attached at all.
 * This keeps the touch event pipeline clean for toasts that should not
 * respond to horizontal drags.
 *
 * The gesture flow:
 * 1. **Touch down** - pause the auto dismiss timer so it doesn't expire mid swipe.
 * 2. **Dragging** - update [horizontalOffset] only in the allowed direction(s).
 *    A [VelocityTracker] records each drag position to compute fling speed.
 *    The offset drives both the card's x position and its opacity.
 * 3. **Release** - dismiss if either condition is met:
 *    - The drag distance exceeds [swipeThresholdPx], OR
 *    - The fling velocity exceeds [VELOCITY_THRESHOLD_PX_PER_SEC] (fast flick)
 *    Otherwise snap back to center and resume the timer.
 */
@ExperimentalToastStackApi
private fun buildSwipeModifier(
    toast: ToastData,
    horizontalOffset: Animatable<Float, *>,
    swipeThresholdPx: Float,
    coroutineScope: CoroutineScope,
    onDismiss: (DismissReason) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit
): Modifier {
    if (toast.swipeDismiss == SwipeDismissDirection.None) return Modifier

    return Modifier.pointerInput(toast.id, toast.swipeDismiss) {
        awaitEachGesture {
            // Wait for the first finger to touch the card.
            val down = awaitFirstDown()
            onPauseTimer()

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            // Track horizontal movement until the finger lifts.
            var dragCancelled = false
            horizontalDrag(down.id) { change ->
                val dragAmount = change.positionChange().x

                // Only permit movement in the direction(s) the toast allows.
                val isAllowed = when (toast.swipeDismiss) {
                    SwipeDismissDirection.Left -> dragAmount < 0 || horizontalOffset.value < 0
                    SwipeDismissDirection.Right -> dragAmount > 0 || horizontalOffset.value > 0
                    SwipeDismissDirection.Both -> true
                    SwipeDismissDirection.None -> false
                }

                if (isAllowed) {
                    coroutineScope.launch {
                        horizontalOffset.snapTo(horizontalOffset.value + dragAmount)
                    }
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                }

                change.consume()
            }

            // Finger lifted or gesture cancelled. Compute the fling velocity
            // to decide whether a fast flick should count as a dismiss even
            // when the drag distance is short.
            val velocity = try {
                velocityTracker.calculateVelocity()
            } catch (_: Exception) {
                // VelocityTracker can throw if it has too few data points.
                null
            }
            val xVelocity = velocity?.x ?: 0f

            // Dismiss if the drag passed the distance threshold OR the
            // fling speed exceeded the velocity threshold.
            val pastDistanceThreshold =
                horizontalOffset.value.absoluteValue > swipeThresholdPx
            val pastVelocityThreshold =
                xVelocity.absoluteValue > VELOCITY_THRESHOLD_PX_PER_SEC

            // For velocity based dismiss, also check that the fling direction
            // matches the allowed swipe direction.
            val velocityDirectionAllowed = when (toast.swipeDismiss) {
                SwipeDismissDirection.Left -> xVelocity < 0
                SwipeDismissDirection.Right -> xVelocity > 0
                SwipeDismissDirection.Both -> true
                SwipeDismissDirection.None -> false
            }

            val shouldDismiss = pastDistanceThreshold ||
                (pastVelocityThreshold && velocityDirectionAllowed)

            if (shouldDismiss) {
                // Fly the card off screen in the direction it was moving.
                val flyDirection = if (horizontalOffset.value >= 0) 1f else -1f
                val flyTarget = flyDirection * size.width.toFloat()
                coroutineScope.launch {
                    horizontalOffset.animateTo(flyTarget, tween(SETTLE_ANIMATION_MILLIS))
                    onDismiss(DismissReason.Swipe)
                }
            } else {
                // Snap back to center and resume the auto dismiss timer.
                coroutineScope.launch {
                    horizontalOffset.animateTo(0f, tween(SETTLE_ANIMATION_MILLIS))
                    onResumeTimer()
                }
            }
        }
    }
}
