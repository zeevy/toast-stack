# ToastStack - Feature Roadmap

A modern, Compose native toast and notification library for Android.
No Scaffold required. One liner API. Stackable. Themeable. Animated.

---

## Phase 1 - Core (v0.1.0)

### Toast Host

- [x] `ToastStackHost` composable that can be placed anywhere in the composition tree
- [x] No dependency on `Scaffold` or `SnackbarHostState`
- [x] Works at the application level (wrap once, use everywhere)
- [x] Multiple hosts supported (e.g., one per screen if needed)
- [x] Host tagging - global `ToastStack.show()` routes to the most recently attached host by default, or to a named host via `ToastStack.show(hostTag = "...")`

### Toast State Management

- [x] `ToastStackState` - central state holder for managing active toasts
- [x] `rememberToastStackState()` - composable state creation (survives configuration changes via `rememberSaveable` or ViewModel scoping)
- [x] Global singleton access via `ToastStack.show()` for ViewModel/non composable callers (API surface grows incrementally as typed methods are added in Phase 2)
- [x] Automatic cleanup of expired toasts
- [x] Maximum visible toast limit (configurable, default 5)

### Basic Toast Display

- [x] Simple text toast with a single function call
- [x] Auto dismiss after configurable duration (short: 2s, long: 4s, indefinite)
- [x] Auto-dismiss timer pauses when app goes to background
- [x] Auto-dismiss timer pauses while user is touching/dragging the toast
- [x] Manual dismiss via close button (optional)
- [x] Toast ID returned from `show()` for programmatic dismissal
- [x] `dismissAll()` to clear all active toasts

### Positioning

- [x] Top center (default)
- [x] Bottom center
- [x] Top start / Top end
- [x] Bottom start / Bottom end
- [x] Center (overlay style)
- [ ] Configurable offset from edges *(host accepts `contentPadding` but per toast custom offset not yet exposed in `ToastData`)*
- [x] Edge-to-edge / WindowInsets support (status bar, navigation bar, display cutouts)
- [x] IME (keyboard) awareness - bottom-positioned toasts shift above the soft keyboard
- [x] RTL layout support - start/end positions and swipe directions respect layout direction

### Basic Swipe to Dismiss

- [x] Swipe left or right to dismiss (default: both directions enabled)
- [x] Swipe direction configurable (left, right, both, none)

### Core Accessibility

- [x] Semantics and `LiveRegion` - toast content announced via TalkBack on show
- [x] Content descriptions on icons and close button
- [ ] Auto-dismiss timer pauses when TalkBack is active *(pauseAll/resumeAll methods exist but automatic TalkBack detection via `AccessibilityManager` not wired up yet)*

### API Stability

- [x] `@ExperimentalToastStackApi` annotation for all pre-1.0 public APIs to signal potential breaking changes

---

## Phase 2 - Styling & Types (v0.2.0)

### Built-in Toast Types

- [x] Default (neutral)
- [x] Success (green accent)
- [x] Error (red accent)
- [x] Warning (amber accent)
- [x] Info (blue accent)

### Typed Convenience Methods

- [x] `ToastStack.success("message")` - one-liner for each type
- [x] `ToastStack.error("message")`
- [x] `ToastStack.warning("message")`
- [x] `ToastStack.info("message")`

### Visual Design

- [x] Material 3 color scheme integration (uses theme colors by default)
- [x] Dynamic color support (Material You) *(Default type uses M3 theme tokens; typed toasts use fixed brand colors with M3 typography)*
- [x] Dark mode support (automatic)
- [x] Rounded card appearance with elevation/shadow
- [x] Leading icon per toast type (configurable)
- [x] Title + description layout (single line or two lines)

### Theming & Customization

- [x] `ToastStackDefaults` object with sensible defaults
- [x] `ToastStackStyle` data class for global style overrides
- [x] Per-toast style override via `show()` parameters
- [x] Custom background color, text color, border, shape
- [x] Custom icon composable slot
- [x] Custom font/typography override

### Memory Safety (added during Phase 2)

- [x] `SupervisorJob` backed coroutine scope prevents orphaned timer leaks
- [x] `destroy()` method cancels all coroutines and clears state
- [x] `rememberToastStackState()` auto calls `destroy()` on composition disposal
- [x] Adaptive toast width: `fillMaxWidth` for center positions, constrained for corners

---

## Phase 3 - Animations (v0.3.0)

### Entry Animations

- [x] Slide in from top (for top positioned toasts)
- [x] Slide in from bottom (for bottom positioned toasts)
- [x] Fade in
- [x] Scale + fade combination
- [x] Configurable animation duration and easing

### Exit Animations

- [x] Slide out (reverse of entry direction)
- [x] Fade out
- [x] Shrink + fade combination
- [x] Height collapse (stack reflows smoothly when a toast is removed)

### Gesture Polish (basic swipe to dismiss is in Phase 1)

- [x] Drag follow (toast follows finger during swipe)
- [x] Opacity fade during swipe (visual feedback)
- [x] Velocity based dismiss (fast flick vs slow drag threshold)

### Stack Animations

- [x] Smooth reordering when a middle toast is dismissed
- [x] New toast pushes existing ones down/up with expandVertically/shrinkVertically
- [x] Stagger effect on initial appearance of multiple toasts

---

## Phase 4 - API Design (v0.4.0)

*Must land before Actions to establish the builder/chaining pattern that
action buttons, progress updates, and suspend variants build on top of.
Prevents breaking API changes later.*

### Builder / Chaining API

- [x] `show()` returns a `ToastHandle` object instead of a raw `String` ID
- [x] `ToastHandle` provides `dismiss()` and chaining methods
- [ ] `ToastStack.show("msg").withAction("Undo") { }` chaining for actions *(deferred to Phase 5 when action buttons are built)*
- [x] `ToastStack.show("msg").onDismiss { reason -> }` chaining for callbacks
- [x] Backward compatible: `ToastHandle.id` property for callers that need the raw ID

### Suspend Function Variant

- [x] `ToastStack.showAndAwait("msg")` suspends until dismissed, returns `DismissReason`
- [x] Coroutine cancellation dismisses the toast automatically
- [x] Works naturally inside `viewModelScope.launch { }`

### Kotlin Duration Support

- [x] `show()` accepts `kotlin.time.Duration` via `ToastDuration(duration)` factory
- [x] `ToastDuration.Custom(millis)` for arbitrary millisecond values
- [x] `ToastDuration` sealed class preserves Short, Long, Indefinite as predefined constants

### Multiplatform Ready Audit

- [x] Audited all public API signatures for Android specific types
- [x] Replaced `java.util.UUID` with `kotlin.uuid.Uuid` (Kotlin 2.x)
- [x] No Android framework types in the public API surface *(only ConcurrentHashMap used internally)*
- [x] `StringResolver` and `ConcurrentHashMap` identified as needing expect/actual for KMP

### String Resource Support

- [x] `show(@StringRes messageRes: Int)` overloads on all show methods
- [x] `show(@StringRes messageRes: Int, vararg formatArgs: Any)` for formatted strings
- [x] Typed methods: `success(@StringRes)`, `error(@StringRes)`, etc.

---

## Phase 5 - Interaction & Actions (v0.5.0)

### Action Buttons

- [ ] Single action button (e.g., "Undo", "Retry", "View")
- [ ] Action callback with toast auto dismiss on click
- [ ] Action button styling (text button, outlined, filled)
- [ ] Secondary action support (two buttons)
- [ ] Chaining API: `.withAction("Undo") { }` built on Phase 4 ToastHandle

### Loading & Progress Toasts

- [ ] Loading toast type with indeterminate progress indicator
- [ ] `ToastStack.loading("message")` returning a `ToastHandle` for later update
- [ ] Determinate progress bar (0-100%)
- [ ] Update progress on an active toast via `handle.updateProgress(0.5f)`
- [ ] Auto transition from loading to success/error on completion
- [ ] Progress text label (e.g., "3 of 10 files uploaded")

### Toast Lifecycle Callbacks

- [ ] `onShow` - called when toast becomes visible
- [ ] `onDismiss` - called when toast is removed (with dismiss reason)
- [ ] `onAction` - called when action button is tapped
- [ ] Chaining API: `.onDismiss { }` and `.onShow { }` built on Phase 4 ToastHandle

### Queueing Behavior

- [ ] When max visible limit is reached, new toasts queue and appear as slots open
- [ ] Priority levels (low, normal, high, urgent)
- [ ] Urgent toasts jump the queue and display immediately
- [ ] Duplicate detection - same message within a time window is ignored or updates the existing toast

---

## Phase 6 - Advanced Features (v0.6.0)

### Custom Content

- [ ] `showCustom()` accepting an arbitrary `@Composable` lambda for fully custom toast layouts
- [ ] Minimum height and width constraints for custom content
- [ ] Custom content still gets swipe to dismiss and auto dismiss behavior

### Stacking Modes

- [ ] Expanded mode (all toasts visible, stacked vertically with spacing)
- [ ] Compact mode (newest toast fully visible, older ones peek behind with offset, like iOS notifications)
- [ ] Tap compact stack to expand (gesture disambiguation with swipe to dismiss: vertical tap vs horizontal swipe)
- [ ] Configurable max stack depth in compact mode

### Haptic Feedback

- [ ] Optional vibration on toast appearance
- [ ] Haptic feedback intensity per toast type (e.g., stronger for errors)
- [ ] Respects system haptic settings

### Advanced Accessibility (basic TalkBack and content descriptions are in Phase 1)

- [ ] Toast type announced (e.g., "Error notification")
- [ ] Action buttons focusable and activatable via accessibility services
- [ ] Sufficient color contrast for all built-in types (WCAG AA)
- [ ] Reduced motion support (simplified animations when system setting is on)

### Sound

- [ ] Optional notification sound on appearance
- [ ] Per type sound customization
- [ ] Respects system Do Not Disturb and silent mode

---

## Phase 7 - Developer Experience (v1.0.0)

### Zero Setup / Auto Initialization

- [ ] `ContentProvider` or `AndroidX Startup Initializer` that auto attaches host to every Activity
- [ ] Users just add the dependency and call `ToastStack.show()` with no composable setup
- [ ] Opt out mechanism for apps that want manual host placement
- [ ] `Modifier.toastStackHost()` as an alternative to `ToastStackHost()` composable

### ViewModel Extensions

- [ ] `ViewModel.showToast()` extension via a provided interface
- [ ] `ViewModel.showToastAndAwait()` suspend extension (builds on Phase 4)
- [ ] Navigation integration: auto dismiss toasts on navigation events

### API Surface

- [ ] `ToastStack.show { ... }` builder DSL for complex toasts
- [ ] `ToastStack.update(id) { ... }` to modify an active toast
- [ ] Stabilize all public APIs - remove `@ExperimentalToastStackApi` for 1.0 release

### Configuration DSL

- [ ] Global configuration via `ToastStackHost(config = ToastStackConfig { ... })`
- [ ] Duration defaults per type
- [ ] Animation defaults
- [ ] Position default
- [ ] Style defaults
- [ ] Max visible count

### Testing Support (140 tests passing, 100% coverage target)

#### ToastStackStateTest (56 tests)

- [x] show adds toast, returns unique non blank ID
- [x] toasts ordered oldest first, defensive copy
- [x] show propagates all parameters (message, title, type, duration, position, closeButton, swipe, style, animation, animationConfig)
- [x] show without optional params defaults to null
- [x] constructor defaults are sensible (maxVisible=5, TopCenter, Short, Both, Slide)
- [x] custom defaults applied when not specified (including animation defaults)
- [x] max visible eviction (oldest removed, onDismiss fires with Programmatic)
- [x] multiple evictions on burst, maxVisible=1 keeps newest
- [x] dismiss by ID, dismiss fires correct reason (Timeout, Swipe, CloseButton, Programmatic)
- [x] dismiss unknown ID is no-op, double dismiss is safe
- [x] dismiss without callback does not crash
- [x] dismissAll removes all, fires onDismiss for each
- [x] dismissAll on empty state, double dismissAll are safe
- [x] destroy clears toasts, destroy on empty is safe, show after destroy is safe
- [x] hostTag defaults to null
- [x] pauseTimer/resumeTimer for unknown ID are safe no-ops
- [x] pauseAll/resumeAll on empty state are safe, do not remove toasts
- [x] indefinite toast is not auto dismissed
- [x] ToastDuration values (Short=2000, Long=4000, Indefinite=MAX_VALUE, 3 entries)
- [x] ToastData defaults (ID, type, duration, position, swipe, all nullable fields)
- [x] ToastData custom ID preserved, two instances have different IDs
- [x] ToastPosition 7 values, SwipeDismissDirection 4 values, DismissReason 4 values
- [x] Typed convenience: success/error/warning/info set correct type
- [x] Typed methods accept title, duration, position, onDismiss

#### ToastStackSingletonTest (23 tests)

- [x] show/success/error/warning/info return null when no host registered
- [x] show routes to most recently registered host
- [x] show routes to specific host via hostTag
- [x] show returns valid toast ID on success
- [x] singleton typed methods set correct ToastType
- [x] singleton typed methods accept title, route to specific host
- [x] singleton show passes type, title, style to state
- [x] dismiss removes toast, dismiss unknown ID is safe
- [x] dismissAll clears all hosts (no tag) or specific host (with tag)
- [x] dismissAll with no hosts is safe
- [x] unregister falls back to remaining host
- [x] unregister all hosts causes show to return null
- [x] register sets hostTag on state

#### ToastStackStyleTest (13 tests)

- [x] all fields default to null
- [x] mergeWith null returns base unchanged
- [x] mergeWith applies non null override fields, leaves null when both null
- [x] mergeWith overrides shape, elevation, border, iconTint, titleColor
- [x] mergeWith overrides titleStyle and messageStyle
- [x] three layer merge (type -> global -> per toast) correct priority
- [x] three layer merge with all null overrides returns base
- [x] ToastStackDefaults.Shape is RoundedCornerShape(12.dp)
- [x] ToastStackDefaults.Elevation is 4.dp

#### ToastTypeTest (7 tests)

- [x] ToastType has 5 values in correct order
- [x] ToastData defaults to Default, preserves explicit type for all 5 types
- [x] title defaults to null, preserves when set
- [x] ToastData.copy preserves all fields

#### ToastAnimationTest (17 tests)

- [x] ToastAnimation has 3 values (Slide, Fade, ScaleAndFade)
- [x] ToastAnimationConfig defaults (enter=300, exit=250, EaseOut, EaseInOut, stagger=50)
- [x] Custom config preserves all values
- [x] show without animation stores null in ToastData
- [x] show with animation/animationConfig stores correctly
- [x] State default animation is Slide, accepts custom defaults
- [x] ToastData animation/animationConfig default to null, preserve explicit values

#### ToastAnimationUiTest (24 Robolectric Compose UI tests)

- [x] Slide enter makes toast visible
- [x] Fade enter makes toast visible
- [x] ScaleAndFade enter makes toast visible
- [x] Slide exit removes toast after dismiss
- [x] Fade exit removes toast after dismiss
- [x] ScaleAndFade exit removes toast after dismiss
- [x] Custom slow enter visible after full duration
- [x] Programmatic dismissAll removes all toasts
- [x] Close button triggers exit animation
- [x] Dismiss middle toast keeps others visible (stack reflow)
- [x] DismissAll removes all toasts after exit animation
- [x] Toast with title shows both title and message
- [x] Toast without title shows only message
- [x] Close button not shown when showCloseButton is false
- [x] Close button shown when showCloseButton is true
- [x] Toast has content description matching message (accessibility)
- [x] Toast with title has combined content description (accessibility)
- [x] Success/Error/Warning/Info icons have content descriptions (accessibility)
- [x] Close button fires onDismiss with CloseButton reason
- [x] Multiple toast types displayed simultaneously
- [x] Max visible enforced in UI (eviction visible)

#### Remaining tests (for future phases)

- [ ] ToastHandle builder/chaining: withAction, onDismiss, dismiss, update (Phase 4)
- [ ] Suspend showAndAwait returns correct DismissReason (Phase 4)
- [ ] Kotlin Duration support alongside ToastDuration enum (Phase 4)
- [ ] String resource overloads resolve correctly (Phase 4)
- [ ] Queue behavior: priority ordering, duplicate detection (Phase 5)
- [ ] Toast lifecycle callbacks: onShow, onAction (Phase 5)
- [ ] Progress toast updates: value changes, auto transition (Phase 5)
- [ ] Loading toast shows indeterminate progress indicator (Phase 5)
- [ ] Action button triggers callback and auto dismisses (Phase 5)
- [ ] Custom content via `showCustom()` with composable lambda (Phase 6)
- [ ] Compact stacking mode (Phase 6)
- [ ] Expanded stacking mode (Phase 6)
- [ ] TalkBack announces toast type (Phase 6)
- [ ] Reduced motion simplified animations (Phase 6)
- [ ] Color contrast meets WCAG AA (Phase 6)
- [ ] Zero setup auto initialization (Phase 7)
- [ ] ViewModel extensions (Phase 7)
- [ ] Modifier.toastStackHost() alternative (Phase 7)

#### Test Infrastructure

- [x] Robolectric for Compose UI tests (no device needed)
- [x] Test utilities for advancing time (Compose mainClock)
- [ ] `TestToastStackState` fake for ViewModel testing (Phase 7)
- [ ] Assertion helpers: `assertToastShown`, `assertNoToasts` (Phase 7)
- [ ] CI pipeline runs full test suite on every PR (Phase 8)

### Documentation & Samples

- [ ] KDoc on every public API
- [ ] Demo app showcasing all features
- [ ] Interactive demo with controls for position, type, duration, animation
- [ ] README with quick start, API reference, and migration guide from Snackbar

---

## Phase 8 - Distribution & Ecosystem (post v1.0)

### Publishing

- [ ] Maven Central publication via `maven-publish` plugin
- [ ] Gradle dependency: `implementation("com.siliconcircuits:toaststack:x.y.z")`
- [ ] Version catalog entry for easy adoption
- [ ] Gradle BOM for version management across future artifacts
- [ ] ProGuard/R8 rules bundled (consumer rules)
- [ ] Source JAR and Javadoc JAR published

### Multiplatform

- [ ] Compose Multiplatform support (Android + Desktop) using expect/actual from Phase 4 audit
- [ ] iOS via Compose Multiplatform (when stable)
- [ ] Common API across all targets

### Integrations

- [ ] Hilt/Koin module for DI setup
- [ ] CI pipeline runs full test suite on every PR

---

## Non Goals

These are explicitly out of scope to keep the library focused:

- Full notification system (use Android NotificationManager for that)
- Persistent notifications that survive process death
- Remote/push notification display
- Dialog replacement (toasts are ephemeral, not blocking)
- Snackbar compatibility layer (this is a replacement, not a wrapper)
