# Changelog

Changes to the ToastStack library, newest first. "Behavior changes" lists what an app can notice without changing its code. "API" lists changes to public signatures; the public API is kept in `toaststack/api/toaststack.api`, and CI fails when it changes without an update.

## Unreleased

### Behavior changes

- Apps need only compileSdk 35, Android Gradle Plugin 8.6 and Kotlin 2.0. 1.3.0 needed compileSdk 37 and AGP 9.1. (#51)
- Every state change runs on the main thread, and callbacks (`onDismiss`, `onShow`, actions) always run on the main thread. Calls from any thread are safe. A `show()` from a background thread returns its handle at once. (#49)
- While TalkBack is on, toasts do not time out, including new and queued ones. Every toast has a "Dismiss" accessibility action, which sends `DismissReason.CloseButton`. (#48)
- Auto overlay toasts survive rotation and other configuration changes. `destroy()` calls `onDismiss(Programmatic)` for every toast, so `await()` returns. (#47)
- `dismissAll()` also calls `onDismiss` of queued toasts. Handle calls on a queued toast now work. (#46)
- A toast dropped because no host is registered logs one warning under the tag `ToastStack`. (#44)
- Start and End positions sit on the correct side in RTL layouts. (#52)
- An urgent toast no longer pushes the stack past `maxVisible`. (#53)
- A custom `contentColor` now also colors the title and the icon. (#54)
- Haptics use the view haptic API ("reject" for errors and warnings, "confirm" for success). The library no longer asks for the VIBRATE permission, so apps lose it from their merged manifest unless they declare it. (#56)
- The screen reader labels, the close button label and the repeat count are string resources an app can translate. The type icon no longer has its own label, so TalkBack reads the card once. (#57)

### Added

- `ToastStack.show()` and `showAndAwait()` take every per-toast option: animation, custom icon, haptic, sound, `soundUri` and priority. (#50)
- `ToastHandle.withSecondaryAction()`, and builder properties for priority, custom icon, `soundUri` and the secondary action. (#50)
- `onDismiss` on the typed string helpers and the ViewModel helpers. (#50)

### Fixed

- The auto overlay keeps its host when another Activity closes, for example after a back press or a splash screen. (#44)
- The auto overlay no longer crashes an Activity that never calls `setContentView`. (#45)

### API

- `ToastStackInitializer` is internal. AndroidX Startup still finds it. (#55)
- `ToastData`'s constructor and `copy()` are internal. Only the library creates toasts. (#55)
- Deprecated: `ToastData.offsetX` and `offsetY` (#50), `ToastStackConfig`, `rememberToastStackState(config)` and `WithToastStack` (#55).
- The library exposes Compose runtime, ui, foundation-layout, animation-core and lifecycle-viewmodel as `api` dependencies, since their types appear in its public API. (#55)
- New parameters on `ToastStack.show`, `showAndAwait` and the helpers change their JVM signatures. Source calls keep compiling. (#50)

## 1.3.0 (2026-09-24)

### Behavior changes

- The auto overlay reads `maxVisible`, position, duration, swipe, animation, the dedup window and the repeat count from `ToastStack` each time a toast is shown, so a later `configure()` call reaches an Activity that is already open.
- `ToastStack.show()`, `showAndAwait()` and `ViewModel.showToast()` use the host's default duration and position when none is given. They used to force `Short` and `TopCenter`.
- `build { }` and `buildToast { }` start from the host defaults, and `buildToast { }` keeps actions, animation and the other builder fields it used to drop.

## 1.2.0 (2026-09-24)

### Behavior changes

- A duplicate of a card on screen restarts its auto dismiss timer and adds one to its repeat count, shown as "(xN)" after the message. A duplicate of a queued toast adds to that toast's count.
- The deduplication window counts from the last repeat, not the first show.

### Added

- `deduplicationWindowMs` and `showRepeatCount` on `ToastStack.configure()`, `rememberToastStackState()` and `ToastStackConfig`.

### API

- `ToastData` has a new `repeatCount` field, and the `ToastStackState` constructor and `rememberToastStackState()` take `showRepeatCount`. These change JVM signatures.

## 1.1.0 (2026-09-22)

### Behavior changes

- Toast colors follow the app's `MaterialTheme` by default (`ToastColorSource.AppTheme`). Pass `ToastColorSource.Library` to keep the fixed colors of 1.0. If no `theme` is passed to `configure()`, the auto overlay uses `Library` and logs one warning.

### Added

- `ToastColorSource`, and `colorSource` and `theme` on `ToastStack.configure()`.

### API

- `ToastStackDefaults.styleForType()` takes a `colorSource` parameter, with no overload for the old signature.
