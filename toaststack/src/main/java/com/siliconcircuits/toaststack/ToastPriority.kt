package com.siliconcircuits.toaststack

/**
 * Priority level that determines how a toast is ordered in the queue
 * when the maximum visible limit is reached.
 *
 * When [ToastStackState.maxVisible] toasts are already on screen, new
 * toasts with [Low] or [Normal] priority enter a queue and appear
 * as existing toasts are dismissed. [High] priority toasts jump ahead
 * in the queue. [Urgent] priority toasts bypass the queue entirely
 * and display immediately, evicting the oldest lower priority toast
 * if necessary.
 *
 * Default priority is [Normal], which queues in FIFO order.
 */
enum class ToastPriority {

    /**
     * Lowest priority. Queued behind Normal and High toasts.
     * Use for non essential notifications that can wait.
     */
    Low,

    /**
     * Default priority. Queued in first in, first out order.
     * Use for standard notifications.
     */
    Normal,

    /**
     * Elevated priority. Jumps ahead of Low and Normal toasts
     * in the queue, but still waits for a slot to open.
     */
    High,

    /**
     * Highest priority. Bypasses the queue and displays immediately.
     * If no slot is available, the oldest lower priority toast is
     * evicted. Use sparingly for critical alerts that demand
     * immediate attention (e.g., connectivity loss, auth expiry).
     */
    Urgent
}
