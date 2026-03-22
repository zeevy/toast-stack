package com.siliconcircuits.toaststack

/**
 * Controls which horizontal swipe directions are permitted for dismissing
 * a toast by dragging it off screen.
 *
 * Swipe gestures are screen relative, not layout relative. This means
 * [Left] always refers to a leftward finger motion on the physical screen,
 * regardless of whether the app is in a left to right (LTR) or right to
 * left (RTL) layout. This keeps the gesture intuitive for the user since
 * they see the card move in the direction their finger moves.
 *
 * When the user's horizontal drag exceeds the internal threshold distance,
 * the toast card animates off screen and is dismissed. If the drag does
 * not reach the threshold, the card snaps back to its original position.
 */
@ExperimentalToastStackApi
enum class SwipeDismissDirection {

    /**
     * Only a leftward swipe (finger moving from right to left) can
     * dismiss the toast. Rightward drags are ignored.
     */
    Left,

    /**
     * Only a rightward swipe (finger moving from left to right) can
     * dismiss the toast. Leftward drags are ignored.
     */
    Right,

    /**
     * The toast can be dismissed by swiping in either direction.
     * This is the default behavior and feels the most natural for
     * most use cases.
     */
    Both,

    /**
     * Swipe to dismiss is completely disabled. The toast can only be
     * removed by tapping the close button (if shown), waiting for the
     * auto dismiss timer, or calling [ToastStackState.dismiss] in code.
     */
    None
}
