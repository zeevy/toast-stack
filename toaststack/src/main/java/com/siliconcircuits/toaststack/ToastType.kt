package com.siliconcircuits.toaststack

/**
 * Semantic category of a toast that determines its default visual appearance.
 *
 * Each type maps to a distinct color scheme and leading icon through
 * [ToastStackDefaults.styleForType]. For example, [Success] renders with
 * a green background and a check mark icon, while [Error] renders with
 * a red background and a close icon.
 *
 * You can override any visual aspect per toast via [ToastStackStyle]
 * while still benefiting from the semantic icon and accessibility label
 * that the type provides.
 *
 * Typed convenience methods on [ToastStackState] and [ToastStack] set
 * the type automatically:
 * ```
 * toastState.success("Upload complete")   // Sets type = Success
 * toastState.error("Connection failed")   // Sets type = Error
 * toastState.warning("Low battery")       // Sets type = Warning
 * toastState.info("Update available")     // Sets type = Info
 * ```
 */
enum class ToastType {

    /**
     * A neutral message with no particular status connotation.
     * Renders without a leading icon, using the Material 3 inverse
     * surface colors (dark card on light theme, light card on dark theme).
     * This is the default type when using [ToastStackState.show] directly.
     */
    Default,

    /**
     * Indicates a positive outcome or successfully completed action.
     * Renders with a green background and a check circle icon.
     * Use for confirmations like "File saved" or "Payment processed".
     */
    Success,

    /**
     * Indicates a failure, validation error, or critical issue.
     * Renders with a red background and a close/error icon.
     * Use for problems like "Upload failed" or "Invalid email address".
     */
    Error,

    /**
     * Indicates a caution or potential problem that the user should
     * be aware of but that does not block their workflow.
     * Renders with an amber/yellow background and a warning triangle icon.
     * Use for situations like "Low storage" or "Connection unstable".
     */
    Warning,

    /**
     * Provides helpful context or a non critical notice.
     * Renders with a blue background and an info circle icon.
     * Use for messages like "New version available" or "Sync complete".
     */
    Info,

    /**
     * Indicates an ongoing operation that hasn't completed yet.
     * Renders with an indeterminate circular progress indicator instead
     * of a static icon. Use with [ToastDuration.Indefinite] and update
     * the toast to [Success] or [Error] when the operation finishes.
     *
     * Example:
     * ```
     * val handle = ToastStack.loading("Uploading...")
     * // Later, when done:
     * handle.dismiss()
     * ToastStack.success("Upload complete")
     * ```
     */
    Loading
}
