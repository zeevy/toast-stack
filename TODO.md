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

- [ ] Slide in from top (for top positioned toasts)
- [ ] Slide in from bottom (for bottom positioned toasts)
- [ ] Fade in
- [ ] Scale + fade combination
- [ ] Configurable animation duration and easing

### Exit Animations

- [ ] Slide out (reverse of entry direction)
- [ ] Fade out
- [ ] Shrink + fade combination
- [ ] Height collapse (stack reflows smoothly when a toast is removed)

### Gesture Polish (basic swipe-to-dismiss is in Phase 1)

- [ ] Drag follow (toast follows finger during swipe)
- [ ] Opacity fade during swipe (visual feedback)
- [ ] Velocity based dismiss (fast flick vs slow drag threshold)

### Stack Animations

- [ ] Smooth reordering when a middle toast is dismissed
- [ ] New toast pushes existing ones down/up with spring animation
- [ ] Stagger effect on initial appearance of multiple toasts

---

## Phase 4 - Interaction & Actions (v0.4.0)

### Action Buttons

- [ ] Single action button (e.g., "Undo", "Retry", "View")
- [ ] Action callback with toast auto-dismiss on click
- [ ] Action button styling (text button, outlined, filled)
- [ ] Secondary action support (two buttons)

### Loading & Progress Toasts

- [ ] Loading toast type with indeterminate progress indicator
- [ ] `ToastStack.loading("message")` returning an ID for later update
- [ ] Determinate progress bar (0-100%)
- [ ] Update progress on an active toast by ID
- [ ] Auto-transition from loading to success/error on completion
- [ ] Progress text label (e.g., "3 of 10 files uploaded")

### Toast Lifecycle Callbacks

- [ ] `onShow` - called when toast becomes visible
- [ ] `onDismiss` - called when toast is removed (with dismiss reason: timeout, swipe, action, programmatic)
- [ ] `onAction` - called when action button is tapped

### Queueing Behavior

- [ ] When max visible limit is reached, new toasts queue and appear as slots open
- [ ] Priority levels (low, normal, high, urgent)
- [ ] Urgent toasts jump the queue and display immediately
- [ ] Duplicate detection - same message within a time window is ignored or updates the existing toast

---

## Phase 5 - Advanced Features (v0.5.0)

### Custom Content

- [ ] `showCustom()` accepting an arbitrary `@Composable` lambda for fully custom toast layouts
- [ ] Minimum height and width constraints for custom content
- [ ] Custom content still gets swipe to dismiss and auto dismiss behavior

### Stacking Modes

- [ ] Expanded mode (all toasts visible, stacked vertically with spacing)
- [ ] Compact mode (newest toast fully visible, older ones peek behind with offset, like iOS notifications)
- [ ] Tap compact stack to expand (gesture disambiguation with swipe-to-dismiss: vertical tap vs horizontal swipe)
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

## Phase 6 - Developer Experience (v1.0.0)

### API Surface (typed convenience methods are in Phase 2, loading API is in Phase 4)

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

### Testing Support (100% Coverage Target)

#### Unit Tests (JUnit + Coroutines Test)

- [ ] `ToastStackState` - show, dismiss, dismissAll, max limit, auto-cleanup
- [ ] Toast ID generation and uniqueness
- [ ] Duration handling (short, long, indefinite, custom)
- [ ] Queue behavior - overflow when max visible reached, priority ordering
- [ ] Duplicate detection logic
- [ ] Toast lifecycle callbacks (onShow, onDismiss, onAction) invocation and dismiss reasons
- [ ] Progress toast updates (value changes, auto-transition on completion)
- [ ] Global singleton state management and thread safety
- [ ] `ToastStackConfig` defaults and overrides
- [ ] `ToastStackStyle` merging (global defaults + per-toast overrides)
- [ ] `ToastStackDefaults` values for each toast type
- [ ] Timer auto-dismiss fires at correct durations
- [ ] Timer pauses/resumes correctly (background, drag, TalkBack)
- [ ] Position enum covers all 6 positions + custom offset
- [ ] Multiple host conflict resolution (which host receives global show)

#### Compose UI Tests (AndroidX Compose Testing)

- [ ] `ToastStackHost` renders toasts in composition tree
- [ ] Toast appears on `show()` and disappears after duration
- [ ] Toast displays correct text, icon, and action button
- [ ] Multiple toasts stack in correct order
- [ ] Positioning - toast appears at correct location for each position
- [ ] Close button dismisses toast on tap
- [ ] Action button triggers callback and auto-dismisses
- [ ] Swipe left/right to dismiss with correct direction config
- [ ] Swipe gesture - drag follow, opacity fade, velocity threshold
- [ ] Entry animations (slide, fade, scale) play on show
- [ ] Exit animations (slide out, fade, shrink, height collapse) play on dismiss
- [ ] Stack reflow animation when middle toast removed
- [ ] Spring animation when new toast pushes existing ones
- [ ] Dark mode renders correct colors
- [ ] Material You / dynamic color integration
- [ ] Custom style overrides (background, text color, border, shape, icon, typography)
- [ ] Title + description layout (single line and two line)
- [ ] Loading toast shows indeterminate progress indicator
- [ ] Determinate progress bar renders and updates
- [ ] Compact stacking mode (peek behind, tap to expand)
- [ ] Expanded stacking mode (all visible with spacing)
- [ ] Custom content via `showCustom()` with composable lambda
- [ ] Edge-to-edge / WindowInsets respected (status bar, nav bar, cutouts)
- [ ] Keyboard (IME) awareness for bottom-positioned toasts
- [ ] RTL layout - positions and swipe directions mirror correctly

#### Accessibility Tests

- [ ] TalkBack announces toast content on show
- [ ] Toast type announced (e.g., "Error notification")
- [ ] Action buttons are focusable and activatable via accessibility services
- [ ] Auto-dismiss pauses when TalkBack is active
- [ ] Color contrast meets WCAG AA for all built-in types
- [ ] Reduced motion - animations simplified when system setting is on
- [ ] Content descriptions set correctly on icons and close button

#### Integration Tests

- [ ] Configuration change survival (rotation, dark mode toggle)
- [ ] Multiple hosts in different screens work independently
- [ ] ViewModel calling global singleton shows toast in host
- [ ] Rapid show/dismiss sequences don't crash or leak
- [ ] Memory - no leaked toasts or coroutines after dismissAll
- [ ] ProGuard/R8 - library works correctly with minification enabled

#### Test Infrastructure

- [ ] `TestToastStackState` fake for unit testing toast emissions from ViewModels
- [ ] Assertion helpers: `assertToastShown(type, message)`, `assertNoToasts()`, `assertToastDismissed(id, reason)`
- [ ] Compose UI test rule integration (`ToastStackTestRule`)
- [ ] Test utilities for advancing time (coroutine test dispatcher)
- [ ] CI pipeline runs full test suite on every PR

### Documentation & Samples

- [ ] KDoc on every public API
- [ ] Demo app showcasing all features
- [ ] Interactive demo with controls for position, type, duration, animation
- [ ] README with quick start, API reference, and migration guide from Snackbar

---

## Phase 7 - Distribution & Ecosystem (post v1.0)

### Publishing

- [ ] Maven Central publication via `maven-publish` plugin
- [ ] Gradle dependency: `implementation("com.siliconcircuits:toaststack:x.y.z")`
- [ ] Version catalog entry for easy adoption
- [ ] ProGuard/R8 rules bundled (consumer rules)
- [ ] Source JAR and Javadoc JAR published

### Multiplatform (Future)

- [ ] Compose Multiplatform support (Android + Desktop)
- [ ] iOS via Compose Multiplatform (when stable)
- [ ] Common API across all targets

### Integrations (Future)

- [ ] ViewModel extension: `ViewModel.showToast()` via a provided interface
- [ ] Navigation integration: auto-dismiss toasts on navigation events
- [ ] Hilt/Koin module for DI setup

---

## Non Goals

These are explicitly out of scope to keep the library focused:

- Full notification system (use Android NotificationManager for that)
- Persistent notifications that survive process death
- Remote/push notification display
- Dialog replacement (toasts are ephemeral, not blocking)
- Snackbar compatibility layer (this is a replacement, not a wrapper)
