# ToastStack

A modern, Compose native toast and notification library for Android.
No Scaffold required. One liner API. Stackable. Themeable. Animated.

[![CI](https://github.com/zeevy/toast-stack/actions/workflows/ci.yml/badge.svg)](https://github.com/zeevy/toast-stack/actions/workflows/ci.yml)
[![CodeQL](https://github.com/zeevy/toast-stack/actions/workflows/codeql.yml/badge.svg)](https://github.com/zeevy/toast-stack/actions/workflows/codeql.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.siliconcircuits/toaststack)](https://central.sonatype.com/artifact/com.siliconcircuits/toaststack)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

<p align="center">
  <img src="docs/images/demo.gif" width="360" alt="Toasts slide in, a progress toast fills up, and the first toast leaves">
</p>

## What is ToastStack?

ToastStack replaces Android's limited native `Toast` and `Snackbar` with a fully featured, Compose native notification system. It works anywhere in your app without requiring `Scaffold`, supports multiple simultaneous toasts with stacking and animations, and provides a one liner API that works from both composables and ViewModels.

## Features

- **6 toast types** - Default, Success, Error, Warning, Info, Loading
- **7 positions** - TopCenter, TopStart, TopEnd, Center, BottomCenter, BottomStart, BottomEnd
- **3 animation styles** - Slide, Fade, ScaleAndFade with configurable duration and easing
- **Action buttons** - Single and secondary actions with auto dismiss
- **Progress toasts** - Indeterminate loading spinner and determinate progress bar
- **Swipe to dismiss** - Configurable direction with velocity based flick detection
- **Custom content** - Arbitrary `@Composable` lambda for fully custom toast layouts
- **Chaining API** - `ToastStack.show("msg")?.withAction("Undo") { }?.onDismiss { }`
- **Suspend support** - `showAndAwait()` suspends until dismissed
- **Kotlin Duration** - `ToastDuration(3.seconds)` for arbitrary durations
- **String resources** - `show(R.string.saved)` with format arguments
- **Builder DSL** - `state.build { message = "..."; type = Success }`
- **ViewModel extensions** - `showToast()`, `showSuccessToast()`, `showToastAndAwait()`
- **Zero setup** - Auto initializer attaches to every Activity, just call `ToastStack.show()`. Leave chosen Activities out with one line
- **App theme** - Toasts follow your app's `MaterialTheme` colors, typography and dark mode. Or switch to the library's own fixed colors
- **Haptic feedback** - Optional haptic per toast type, with no VIBRATE permission
- **Sound** - Optional notification sound, or your own sound per toast with `soundUri`
- **Accessibility** - TalkBack announcements with type prefixes, timers that stop while TalkBack is on, a Dismiss action, translatable labels, reduced motion support, WCAG AA contrast
- **Priority queue** - Low, Normal, High, Urgent with queue overflow handling
- **Duplicate detection** - Repeats of a message within a time window go into one card with an "(xN)" count
- **RTL support** - Start/End positions mirror correctly
- **Edge to edge** - Respects system bars, display cutouts, and software keyboard
- **Thread safe** - Call it from any thread; callbacks run on the main thread
- **Survives rotation** - Toasts and their handles stay through configuration changes
- **Older setups** - Works with compileSdk 35, Android Gradle Plugin 8.6 and Kotlin 2.0

## Screenshots

Toasts follow your app's Material colors by default, in light and dark themes:

<p align="center">
  <img src="docs/images/types-light.png" width="48%" alt="Success, error, warning, info and default toasts in a light theme">
  <img src="docs/images/types-dark.png" width="48%" alt="The same toasts in a dark theme">
</p>

Or use the library's own fixed colors with `ToastColorSource.Library`:

<p align="center">
  <img src="docs/images/library-light.png" width="48%" alt="Toasts in the library's fixed green, red, amber and blue colors, light theme">
  <img src="docs/images/library-dark.png" width="48%" alt="The same toasts on a dark background">
</p>

Action buttons, a progress bar and a close button:

<p align="center">
  <img src="docs/images/actions-light.png" width="48%" alt="Toasts with Undo, Retry and View actions, an upload progress bar and a close button, light theme">
  <img src="docs/images/actions-dark.png" width="48%" alt="The same toasts in a dark theme">
</p>

Seven positions; Start and End swap sides in right to left layouts:

<p align="center">
  <img src="docs/images/positions-light.png" width="96%" alt="Toasts at top start, top end, center, bottom start and bottom end over an inbox screen">
</p>

Any composable as the toast body:

<p align="center">
  <img src="docs/images/custom-light.png" width="48%" alt="A custom toast with an avatar, a name and a line of text, light theme">
  <img src="docs/images/custom-dark.png" width="48%" alt="The same custom toast in a dark theme">
</p>

## Installation

ToastStack is on [Maven Central](https://central.sonatype.com/artifact/com.siliconcircuits/toaststack). Check the [Requirements](#requirements) first.

**1. Repository.** `mavenCentral()` must be in your `settings.gradle.kts`. New Android Studio projects already have it:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

**2. Dependency.** Add the library to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.siliconcircuits:toaststack:1.5.0")
}
```

Or with a version catalog (`gradle/libs.versions.toml`):

```toml
[versions]
toastStack = "1.5.0"

[libraries]
toast-stack = { group = "com.siliconcircuits", name = "toaststack", version.ref = "toastStack" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.toast.stack)
}
```

<details>
<summary>Groovy (<code>build.gradle</code>)</summary>

```groovy
dependencies {
    implementation 'com.siliconcircuits:toaststack:1.5.0'
}
```

</details>

**3. Sync Gradle.** There is no setup code. The library starts itself with the app, so you can call `ToastStack.success("Saved")` from your code. See [Quick Start](#quick-start).

### Moving from JitPack

Versions up to 1.4.0 were only on JitPack. To move to Maven Central:

1. Change `com.github.zeevy:toast-stack:v1.4.0` to `com.siliconcircuits:toaststack:1.5.0`. The version has no `v`.
2. Remove `maven { url = uri("https://jitpack.io") }` if no other library needs it.
3. Do not keep both. They hold the same classes, so the build fails with duplicate class errors.

The package name stays `com.siliconcircuits.toaststack`, so no imports change.

## Quick Start

ToastStack auto initializes. Just add the dependency and show toasts from anywhere:

```kotlin
// One liners from anywhere (ViewModel, callback, service)
ToastStack.success("File saved")
ToastStack.error("Upload failed")
ToastStack.warning("Low battery")
ToastStack.info("Update available")
ToastStack.show("Plain message")
```

That's it. No `Scaffold`, no `SnackbarHostState`, no `setContent` wiring.

These calls are safe from any thread. The toast is added on the main thread, and callbacks such as `onDismiss` and actions run on the main thread.

The toast goes to the Activity on screen. These calls return null and drop the toast when no host is registered: before the first Activity resumes, or when `hostTag` names a host that does not exist. The first drop logs one warning under the tag `ToastStack`.

### Global Configuration (optional)

Customize defaults once in your `Application.onCreate()`. The auto-initializer picks these up when attaching to each Activity:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ToastStack.configure(
            defaultPosition = ToastPosition.TopCenter,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 98.dp,
                bottom = 8.dp,
            ),
            defaultDuration = ToastDuration.Short,
            maxVisible = 5,
            defaultSwipeDismiss = SwipeDismissDirection.Both,
            defaultAnimation = ToastAnimation.Slide,
            defaultAnimationConfig = ToastAnimationConfig(),
            deduplicationWindowMs = 0, // e.g. 3000 to merge repeats into one card
            globalStyle = null, // or a ToastStackStyle for app-wide look
            colorSource = ToastColorSource.AppTheme,
            theme = { content -> MyAppTheme { content() } },
        )
    }
}
```

All parameters are optional and have sensible defaults. You can call `configure()` again later, for example when the user changes a toast time setting. The next toast uses the new values, and a toast already on screen keeps its own duration. `contentPadding` and `globalStyle` are the exception: they are read only when an Activity's overlay is first created.

### Theming

Toast colors come from one of two sources, set with `colorSource`:

- `ToastColorSource.AppTheme` (default): toasts render inside your app theme. Colors and typography come from your `MaterialTheme`, so dark mode, dynamic color and in app theme switches follow automatically. Typed toasts map to Material 3 color roles:

| Type | Background | Content |
| --- | --- | --- |
| Default, Loading | `inverseSurface` | `inverseOnSurface` |
| Success | `primaryContainer` | `onPrimaryContainer` |
| Info | `secondaryContainer` | `onSecondaryContainer` |
| Warning | `tertiaryContainer` | `onTertiaryContainer` |
| Error | `errorContainer` | `onErrorContainer` |

- `ToastColorSource.Library`: the library's own colors. Default and Loading use the baseline Material 3 inverse surface. Success, Error, Warning and Info use fixed green, red, amber and blue.

The auto overlay is a separate Compose tree, so it cannot see your theme by itself. Pass your theme composable as `theme` in `ToastStack.configure()`. If `colorSource` is `AppTheme` and no `theme` is given, the overlay falls back to `Library` and logs one warning. A `ToastStackHost` placed by hand inside your Compose tree already sits under your theme and needs no `theme` parameter.

Two rules for the `theme` wrapper:

- It must only set the theme. Do not put `Surface`, `Scaffold` or any background inside it. The overlay covers the whole Activity, so a background there hides your app's UI. If your theme function wraps `Surface`, pass a version without it, for example `{ content -> MaterialTheme(colorScheme = myScheme, typography = myTypography, content = content) }`.
- Call `ToastStack.configure()` from `Application.onCreate()`. The lambda is kept for the life of the process, so do not capture an Activity, View, ViewModel or Activity Context in it.

Upgrading from 1.0.x: `AppTheme` is the default, so a `ToastStackHost` you place by hand now shows typed toasts in your theme's color roles instead of the fixed green, red, amber and blue. To keep the old look, pass `colorSource = ToastColorSource.Library` to the host or set it in `ToastStack.configure()`.

Material 3 has no success role, so Success uses `primaryContainer`. If you want a green success toast, override it with `globalStyle` or a per toast style. Global style and per toast style still apply on top of either source.

### Leaving an Activity out

The auto overlay goes into every `ComponentActivity`. To leave one out, for example a third-party login, payment or ad screen, pass a filter in `Application.onCreate()`:

```kotlin
ToastStack.configure(autoOverlay = { activity -> activity !is PaymentActivity })
```

To turn the overlay off everywhere, remove `ToastStackInitializer` in your manifest with `tools:node="remove"` and place `ToastStackHost` yourself. A hand-placed host with the default tag in an Activity that also has the overlay logs one warning, because both would take toasts sent without a `hostTag`.

## API Overview

### Chaining

```kotlin
ToastStack.show("Item deleted", duration = ToastDuration.Long)
    ?.withAction("Undo") { restoreItem() }
    ?.onDismiss { reason -> log("dismissed: $reason") }
```

### Suspend

```kotlin
viewModelScope.launch {
    val reason = ToastStack.showAndAwait(
        "Confirm delete?",
        showCloseButton = true
    )
    if (reason == DismissReason.Action) undoDelete()
}
```

### Title and Description

```kotlin
ToastStack.error(
    "Connection timed out. Please check your network.",
    title = "Network Error"
)
```

### Custom Duration

```kotlin
import kotlin.time.Duration.Companion.seconds

ToastStack.show("Gone in 3 seconds", duration = ToastDuration(3.seconds))
ToastStack.show("Custom millis", duration = ToastDuration.Custom(1500))
```

### Loading and Progress

```kotlin
val handle = ToastStack.loading("Uploading files...")

// Update progress
handle?.updateProgress(0.5f, "5 of 10 files")

// Complete
handle?.dismiss()
ToastStack.success("Upload complete")
```

### Action Buttons

```kotlin
ToastStack.show("Message sent")
    ?.withAction("Undo") {
        unsendMessage()
    }
    ?.withSecondaryAction("View") { openMessage() }
```

### Custom Content

```kotlin
toastState.showCustom(
    duration = ToastDuration.Long,
    showCloseButton = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(R.drawable.avatar), contentDescription = null)
        Column {
            Text("Custom Layout", fontWeight = FontWeight.Bold)
            Text("Any composable content you want")
        }
    }
}
```

### Builder DSL

```kotlin
toastState.build {
    message = "Connection lost"
    type = ToastType.Error
    duration = ToastDuration.Long
    showCloseButton = true
    hapticEnabled = true
    actionLabel = "Retry"
    onAction = { reconnect() }
    onDismiss = { reason -> log(reason) }
}
```

### ViewModel Extensions

```kotlin
class MyViewModel : ViewModel() {
    fun onSaveComplete() {
        showSuccessToast("Document saved")
    }

    fun onError(message: String) {
        showErrorToast(message, title = "Error")
    }

    fun confirmDelete() {
        showToastAndAwait(
            "Item will be deleted",
            showCloseButton = true
        ) { reason ->
            if (reason == DismissReason.Timeout) performDelete()
        }
    }
}
```

### String Resources

```kotlin
ToastStack.show(R.string.file_saved)
ToastStack.error(R.string.upload_failed, fileName)
```

## Configuration

Global defaults are configured via `ToastStack.configure()` in your `Application.onCreate()` (see [Global Configuration](#global-configuration-optional) above).

### Per Toast Styling

```kotlin
ToastStack.show(
    message = "Custom look",
    style = ToastStackStyle(
        backgroundColor = Color(0xFF6A1B9A),
        contentColor = Color.White,
        borderColor = Color.White,
        borderWidth = 2.dp,
        shape = RoundedCornerShape(24.dp)
    )
)
```

### Animations

```kotlin
// Per toast animation override
ToastStack.show(
    "Fade in",
    animation = ToastAnimation.Fade,
    animationConfig = ToastAnimationConfig(
        enterDurationMillis = 500,
        exitDurationMillis = 300
    )
)
```

### Priority Queue

```kotlin
// Normal toasts queue when maxVisible is reached
ToastStack.show("Queued", priority = ToastPriority.Normal)

// Urgent toasts bypass the queue and show immediately
ToastStack.show("Critical!", priority = ToastPriority.Urgent)
```

### Duplicate Detection

```kotlin
// Global host (auto overlay): set it in Application.onCreate()
ToastStack.configure(deduplicationWindowMs = 3000)

// Or on your own state
val state = ToastStackState(deduplicationWindowMs = 3000)
state.show("Same message")
state.show("Same message") // No new card. The first card shows "Same message (x2)"
```

While the card is still on screen, each repeat within the window restarts its auto dismiss timer and adds one to the count. The same message after the card has gone shows a new card. Pass `showRepeatCount = false` to hide the "(xN)" count.

## Haptic and Sound

```kotlin
ToastStack.show(
    "Feel the vibration",
    hapticEnabled = true,  // Vibrates on appear
    soundEnabled = true    // Plays notification sound
)
```

The haptic depends on the type: Error and Warning use the system "reject" haptic and Success the "confirm" haptic (Android 11 and newer; older versions use a long press or key tap), other types a light key tap. It uses the view haptic API, so the app needs no VIBRATE permission.
Both features respect system settings (haptic toggle, silent mode, Do Not Disturb).

## Accessibility

- **TalkBack**: Toasts announce with type prefix (e.g., "Error notification: Connection failed")
- **Auto pause**: Auto dismiss timers stop while TalkBack is on, for toasts already on screen and for new ones, so each toast stays until it is dismissed. Every toast has a "Dismiss" accessibility action, so it can be closed without a close button
- **Reduced motion**: Animations simplify to a short fade when system "Remove animations" is enabled
- **Color contrast**: All built in types meet WCAG AA 4.5:1 minimum ratio
- **Focus**: Action buttons are focusable and activatable via accessibility services
- **Translations**: The screen reader prefixes, the close button label and the repeat count are string resources (`toaststack_type_success`, `toaststack_type_error`, `toaststack_type_warning`, `toaststack_type_info`, `toaststack_type_loading`, `toaststack_dismiss`, `toaststack_dismiss_action` and the plural `toaststack_repeat_count`). Give a string with the same name in your app's `res/values-xx/strings.xml` to translate it.

## ProGuard / R8

Consumer rules are bundled with the library. No additional ProGuard configuration is needed.

## Requirements

- Min SDK 24 (Android 7.0)
- Compile SDK 35 or higher
- Android Gradle Plugin 8.6 or higher
- Kotlin 2.0 or higher
- Jetpack Compose with Material 3. The library brings Compose 1.11 and Material 3 1.4 at runtime, so an app on older versions is moved up to these.

CI builds a small app on exactly these minimums against every change.

## Changelog

What changed in each release, including behavior changes, is in [CHANGELOG.md](CHANGELOG.md).

## Contributing

Contributions of any size are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the development setup, coding conventions, and how to run the lint/test suite locally before opening a pull request. By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

- **Bugs and feature requests**: open an [issue](https://github.com/zeevy/toast-stack/issues) using the templates.
- **Questions and ideas**: start a [discussion](https://github.com/zeevy/toast-stack/discussions).
- **Security reports**: follow the [Security Policy](SECURITY.md) - please do not file public issues for vulnerabilities.

## License

```text
Copyright 2026 zeevy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
