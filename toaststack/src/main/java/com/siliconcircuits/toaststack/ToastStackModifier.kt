package com.siliconcircuits.toaststack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wraps [content] with a [ToastStackHost] overlay, providing an
 * alternative to placing [ToastStackHost] as a separate sibling.
 *
 * Instead of:
 * ```
 * Box {
 *     MyScreen()
 *     ToastStackHost(state = state)
 * }
 * ```
 *
 * You can write:
 * ```
 * WithToastStack(state = state) {
 *     MyScreen()
 * }
 * ```
 *
 * This is a convenience wrapper that handles the [Box] layering for
 * you. The toast overlay is drawn on top of the content.
 *
 * @param state The state holder that owns the toast list. When omitted,
 *   a default [rememberToastStackState] is created and the auto
 *   initialization system handles registration.
 * @param tag Host tag for [ToastStack] singleton routing.
 * @param globalStyle Optional style applied to all toasts in this host.
 * @param content The composable content to overlay toasts on.
 */
@Composable
fun WithToastStack(
    state: ToastStackState = rememberToastStackState(),
    tag: String = "__toaststack_wrapper__",
    modifier: Modifier = Modifier,
    globalStyle: ToastStackStyle? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        ToastStackHost(
            state = state,
            tag = tag,
            globalStyle = globalStyle,
            contentPadding = contentPadding
        )
    }
}
