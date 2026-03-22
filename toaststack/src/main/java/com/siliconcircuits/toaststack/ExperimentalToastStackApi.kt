package com.siliconcircuits.toaststack

/**
 * Marks ToastStack declarations that are still evolving and may change
 * before the library reaches version 1.0.
 *
 * In Kotlin, the `@RequiresOptIn` mechanism forces consumers to explicitly
 * acknowledge that they are using an unstable API. Without opting in, the
 * compiler emits a warning (or error, depending on the level) so that
 * developers are not surprised by breaking changes in future releases.
 *
 * To use any ToastStack API annotated with this marker, add one of:
 * - `@OptIn(ExperimentalToastStackApi::class)` on the calling function or class
 * - `@file:OptIn(ExperimentalToastStackApi::class)` at the top of the file
 *
 * Once the library reaches 1.0, this annotation will be removed and all
 * public APIs will follow standard semantic versioning guarantees.
 */
@RequiresOptIn(
    message = "This API is experimental and may change in future releases.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class ExperimentalToastStackApi
