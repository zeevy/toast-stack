package com.siliconcircuits.toaststack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import java.util.concurrent.atomic.AtomicBoolean
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
 * @param colorSource Where the type default colors come from.
 * @param onDismiss Callback invoked when the toast should be removed, with the
 *   [DismissReason] indicating what triggered the dismissal.
 * @param onPauseTimer Pauses the auto dismiss countdown while the user is
 *   interacting with the card.
 * @param onResumeTimer Resumes the countdown after the interaction ends
 *   without a dismiss.
 */
@Composable
internal fun ToastItem(
    toast: ToastData,
    globalStyle: ToastStackStyle? = null,
    colorSource: ToastColorSource = ToastStack.colorSource,
    onDismiss: (DismissReason) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val horizontalOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }

    // Resolve the effective style by layering: type defaults -> global -> per toast.
    val typeDefaults = ToastStackDefaults.styleForType(toast.type, colorSource)
    val resolvedStyle = typeDefaults.mergeWith(globalStyle).mergeWith(toast.style)
    val contentColor = resolvedStyle.contentColor ?: typeDefaults.contentColor!!

    // True while a finger holds the card. If the card leaves the screen
    // mid-gesture (for example on rotation), the release code never runs,
    // so resume the timer here or the toast would never time out.
    val held = remember { AtomicBoolean(false) }
    DisposableEffect(toast.id) {
        onDispose { if (held.get()) onResumeTimer() }
    }

    val swipeModifier = buildSwipeModifier(
        toast = toast,
        horizontalOffset = horizontalOffset,
        swipeThresholdPx = swipeThresholdPx,
        coroutineScope = coroutineScope,
        onDismiss = onDismiss,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        held = held
    )

    // Gradually reduce opacity as the card moves further from center,
    // giving the user visual feedback that releasing will dismiss.
    val dragAlpha = if (swipeThresholdPx > 0f) {
        (1f - (horizontalOffset.value.absoluteValue / (swipeThresholdPx * 1.5f)))
            .coerceIn(0.3f, 1f)
    } else {
        1f
    }

    val messageText = messageText(toast)
    val accessibilityLabel = accessibilityLabel(toast, messageText)
    val dismissLabel = stringResource(R.string.toaststack_dismiss)
    val dismissActionLabel = stringResource(R.string.toaststack_dismiss_action)

    // The border is drawn only when the style sets a border color.
    val borderStroke = resolvedStyle.borderColor?.let { BorderStroke(resolvedStyle.borderWidth ?: 1.dp, it) }

    // Centered positions fill the width like a snackbar. Corner cards wrap
    // their content, so the Start and End sides are visible on screen.
    val widthModifier = if (toast.position.fillsWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.widthIn(min = 200.dp, max = 320.dp)
    }

    // offsetX/offsetY are deprecated and still honoured until they are removed.
    @Suppress("DEPRECATION")
    val offset = Modifier.offset(x = toast.offsetX.dp, y = toast.offsetY.dp)

    Box(modifier = Modifier.padding(vertical = 4.dp).then(offset)) {
        Surface(
            modifier = widthModifier
                .offset { IntOffset(horizontalOffset.value.roundToInt(), 0) }
                .alpha(dragAlpha)
                .then(swipeModifier)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = accessibilityLabel
                    // Timers stop while TalkBack is on, so give its users a way
                    // to close any toast, with or without a close button.
                    customActions = listOf(
                        CustomAccessibilityAction(dismissActionLabel) {
                            onDismiss(DismissReason.CloseButton)
                            true
                        }
                    )
                },
            shape = resolvedStyle.shape ?: ToastStackDefaults.Shape,
            shadowElevation = resolvedStyle.elevation ?: ToastStackDefaults.Elevation,
            tonalElevation = 2.dp,
            color = resolvedStyle.backgroundColor ?: typeDefaults.backgroundColor!!,
            border = borderStroke
        ) {
            Row(
                // Custom content gets a minimum height so it stays visible and tappable.
                modifier = Modifier
                    .then(if (toast.customContent != null) Modifier.defaultMinSize(minHeight = 48.dp) else Modifier)
                    .padding(start = 12.dp, end = if (toast.showCloseButton) 4.dp else 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (toast.customContent != null) {
                    Box(modifier = Modifier.weight(1f)) { toast.customContent.invoke() }
                } else {
                    DefaultToastContent(toast, resolvedStyle, contentColor, messageText, onDismiss)
                }
                if (toast.showCloseButton) {
                    CloseButton(label = dismissLabel, tint = contentColor) { onDismiss(DismissReason.CloseButton) }
                }
            }
        }
    }
}

/**
 * The message as shown. Deduplication counts repeats of the same message,
 * so "(x5)" tells the user the event happened more than once. The text is
 * a plural resource, so apps can translate it.
 */
@Composable
private fun messageText(toast: ToastData): String = if (toast.repeatCount > 1) {
    pluralStringResource(R.plurals.toaststack_repeat_count, toast.repeatCount, toast.message, toast.repeatCount)
} else {
    toast.message
}

/**
 * What a screen reader reads for the card, with the type first, for example
 * "Error notification: Connection failed". The prefixes are string
 * resources, so apps can translate them.
 */
@Composable
internal fun accessibilityLabel(toast: ToastData, messageText: String): String {
    val typePrefix = when (toast.type) {
        ToastType.Success -> stringResource(R.string.toaststack_type_success)
        ToastType.Error -> stringResource(R.string.toaststack_type_error)
        ToastType.Warning -> stringResource(R.string.toaststack_type_warning)
        ToastType.Info -> stringResource(R.string.toaststack_type_info)
        ToastType.Loading -> stringResource(R.string.toaststack_type_loading)
        ToastType.Default -> null
    }
    return buildString {
        if (typePrefix != null) append(typePrefix).append(": ")
        if (toast.title != null) append(toast.title).append(". ")
        append(messageText)
    }
}

/** The leading visual, the text and the action buttons of a standard toast. */
@Composable
private fun RowScope.DefaultToastContent(
    toast: ToastData,
    style: ToastStackStyle,
    contentColor: Color,
    messageText: String,
    onDismiss: (DismissReason) -> Unit,
) {
    // Leading visual: loading spinner, custom icon, or type icon. The spinner
    // shows for Loading until a progress value is set; then the progress bar
    // below the text takes over.
    val showSpinner = toast.type == ToastType.Loading && toast.customIcon == null && toast.progress == null
    val typeIcon = iconForType(toast.type)
    val leading: (@Composable (Modifier) -> Unit)? = when {
        showSpinner -> { modifier -> CircularProgressIndicator(modifier = modifier, color = contentColor, strokeWidth = 2.dp) }
        toast.customIcon != null -> { modifier -> Box(modifier, contentAlignment = Alignment.Center) { toast.customIcon.invoke() } }
        typeIcon != null -> { modifier ->
            Icon(
                imageVector = typeIcon,
                // The type is already in the card's label, so the icon is decorative.
                contentDescription = null,
                tint = style.iconTint ?: contentColor,
                modifier = modifier.testTag("toaststack_type_icon")
            )
        }
        else -> null
    }

    // Text area: the leading visual starts the first line (the title, or the
    // message when there is no title), so every other line uses the full width.
    Column(modifier = Modifier.weight(1f)) {
        if (toast.title != null) {
            TextWithLeading(toast.title, leading, style.titleStyle!!, style.titleColor ?: contentColor)
            Text(text = messageText, style = style.messageStyle!!, color = contentColor)
        } else {
            TextWithLeading(messageText, leading, style.messageStyle!!, contentColor)
        }
        if (toast.progress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { toast.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.3f)
            )
        }
        if (toast.progressLabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = toast.progressLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }

    // Actions: the primary one, then the optional secondary one, dimmer.
    if (toast.actionLabel != null) {
        Spacer(modifier = Modifier.width(8.dp))
        ActionButton(toast.actionLabel, contentColor) {
            toast.onAction?.invoke()
            onDismiss(DismissReason.Action)
        }
    }
    if (toast.secondaryActionLabel != null) {
        ActionButton(toast.secondaryActionLabel, contentColor.copy(alpha = 0.8f)) {
            toast.onSecondaryAction?.invoke()
            onDismiss(DismissReason.Action)
        }
    }
}

/**
 * [text] with [leading] drawn at the start of its first line. The icon slot is
 * a square one line high (1.43em, the line height of the default styles), so
 * the icon grows with the font size and stays centred on the line. A bigger
 * custom icon is shrunk to fit the slot.
 *
 * The icon and the gap after it are two stand-in characters in the text.
 * Screen readers read the plain text, but TalkBack's move by line or by
 * character is off by these two characters.
 */
@Composable
private fun TextWithLeading(text: String, leading: (@Composable (Modifier) -> Unit)?, style: TextStyle, color: Color) {
    if (leading == null) {
        Text(text = text, style = style, color = color)
        return
    }
    val icon = InlineTextContent(Placeholder(1.43.em, 1.43.em, PlaceholderVerticalAlign.TextCenter)) {
        leading(Modifier.fillMaxSize())
    }
    // The gap is its own slot, so it stays between the icon and the text in
    // both text directions, also when the text direction is not the layout's.
    val gap = InlineTextContent(Placeholder(0.57.em, 1.43.em, PlaceholderVerticalAlign.TextCenter)) {}
    Text(
        text = buildAnnotatedString {
            appendInlineContent(ICON_SLOT, "*")
            appendInlineContent(GAP_SLOT, "*")
            append(text)
        },
        // Screen readers get the plain text, without the stand-in characters.
        modifier = Modifier.semantics { this.text = AnnotatedString(text) },
        style = style,
        color = color,
        inlineContent = mapOf(ICON_SLOT to icon, GAP_SLOT to gap)
    )
}

private const val ICON_SLOT = "icon"
private const val GAP_SLOT = "gap"

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

/** The close (X) button on the trailing edge of the card. */
@Composable
private fun CloseButton(label: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(imageVector = Icons.Default.Close, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
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
private fun iconForType(type: ToastType): ImageVector? {
    return when (type) {
        ToastType.Default -> null
        ToastType.Success -> Icons.Default.CheckCircle
        ToastType.Error -> Icons.Default.Close
        ToastType.Warning -> Icons.Default.Warning
        ToastType.Info -> Icons.Default.Info
        ToastType.Loading -> null // Loading uses CircularProgressIndicator instead.
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
private fun buildSwipeModifier(
    toast: ToastData,
    horizontalOffset: Animatable<Float, *>,
    swipeThresholdPx: Float,
    coroutineScope: CoroutineScope,
    onDismiss: (DismissReason) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    held: AtomicBoolean
): Modifier {
    if (toast.swipeDismiss == SwipeDismissDirection.None) return Modifier

    return Modifier.pointerInput(toast.id, toast.swipeDismiss) {
        awaitEachGesture {
            // Wait for the first finger to touch the card.
            val down = awaitFirstDown()
            onPauseTimer()
            held.set(true)

            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            // Track horizontal movement until the finger lifts.
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
                    held.set(false)
                    onDismiss(DismissReason.Swipe)
                }
            } else {
                // Snap back to center and resume the auto dismiss timer.
                coroutineScope.launch {
                    horizontalOffset.animateTo(0f, tween(SETTLE_ANIMATION_MILLIS))
                    held.set(false)
                    onResumeTimer()
                }
            }
        }
    }
}
