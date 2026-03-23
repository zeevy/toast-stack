package com.siliconcircuits.toaststack

import androidx.compose.runtime.Composable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Immutable snapshot of a single toast's configuration.
 *
 * Created internally by [ToastStackState.show] and consumed by the
 * host composable for rendering. The [id] is auto generated and
 * returned to the caller for later programmatic dismissal or updates.
 *
 * @property id Unique identifier, generated via [UUID] to avoid collisions
 *   across rapid sequential calls.
 * @property message Primary text displayed inside the toast.
 * @property title Optional bold headline rendered above [message].
 *   When null, only the message line is shown.
 * @property type Semantic category that determines default colors and icon.
 * @property duration How long the toast stays visible before auto dismissing.
 * @property position Screen anchor where this toast is rendered.
 * @property showCloseButton Whether a dismiss button appears on the trailing edge.
 * @property swipeDismiss Allowed horizontal swipe directions for manual dismissal.
 * @property style Optional per toast style overrides. When null, the type's
 *   defaults from [ToastStackDefaults] are used.
 * @property animation The enter/exit animation style for this toast.
 *   When null, falls back to the host's default animation.
 * @property animationConfig Timing and easing overrides for this toast's
 *   animation. When null, falls back to the host's default config.
 * @property actionLabel Text for the primary action button (e.g., "Undo").
 *   When null, no action button is rendered.
 * @property onAction Callback invoked when the user taps the action button.
 *   The toast is auto dismissed with [DismissReason.Action] after this fires.
 * @property secondaryActionLabel Text for an optional second action button.
 * @property onSecondaryAction Callback for the secondary action button.
 * @property progress Current progress value for determinate progress toasts.
 *   Range is 0f to 1f. When null, no progress bar is shown (unless the type
 *   is Loading, which shows an indeterminate indicator).
 * @property progressLabel Optional text shown alongside the progress bar
 *   (e.g., "3 of 10 files").
 * @property customIcon Optional composable that replaces the default type icon.
 * @property onDismiss Optional callback invoked when the toast is removed,
 *   with the [DismissReason] explaining why.
 */
data class ToastData(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    val message: String,
    val title: String? = null,
    val type: ToastType = ToastType.Default,
    val duration: ToastDuration = ToastDuration.Short,
    val position: ToastPosition = ToastPosition.TopCenter,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val showCloseButton: Boolean = false,
    val swipeDismiss: SwipeDismissDirection = SwipeDismissDirection.Both,
    val style: ToastStackStyle? = null,
    val animation: ToastAnimation? = null,
    val animationConfig: ToastAnimationConfig? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val secondaryActionLabel: String? = null,
    val onSecondaryAction: (() -> Unit)? = null,
    val progress: Float? = null,
    val progressLabel: String? = null,
    val priority: ToastPriority = ToastPriority.Normal,
    val customIcon: (@Composable () -> Unit)? = null,
    val customContent: (@Composable () -> Unit)? = null,
    val hapticEnabled: Boolean = false,
    val soundEnabled: Boolean = false,
    val soundUri: android.net.Uri? = null,
    val onShow: (() -> Unit)? = null,
    val onDismiss: ((DismissReason) -> Unit)? = null
)
