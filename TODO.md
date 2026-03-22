# ToastStack - Feature Roadmap

A modern, Compose-native toast and notification library for Android.
No Scaffold required. One-liner API. Stackable. Themeable. Animated.

---

## Phase 1 - Core (v0.1.0)

### Toast Host

- [ ] `ToastStackHost` composable that can be placed anywhere in the composition tree
- [ ] No dependency on `Scaffold` or `SnackbarHostState`
- [ ] Works at the application level (wrap once, use everywhere)
- [ ] Multiple hosts supported (e.g., one per screen if needed)

### Toast State Management

- [ ] `ToastStackState` - central state holder for managing active toasts
- [ ] `rememberToastStackState()` - composable state creation
- [ ] Global singleton access via `ToastStack.show()` for ViewModel/non-composable callers
- [ ] Automatic cleanup of expired toasts
- [ ] Maximum visible toast limit (configurable, default 5)

### Basic Toast Display

- [ ] Simple text toast with a single function call
- [ ] Auto-dismiss after configurable duration (short: 2s, long: 4s, indefinite)
- [ ] Manual dismiss via close button (optional)
- [ ] Toast ID returned from `show()` for programmatic dismissal
- [ ] `dismissAll()` to clear all active toasts

### Positioning

- [ ] Top center (default)
- [ ] Bottom center
- [ ] Top start / Top end
- [ ] Bottom start / Bottom end
- [ ] Center (overlay style)
- [ ] Configurable offset from edges

---

## Phase 2 - Styling & Types (v0.2.0)

### Built-in Toast Types

- [ ] Default (neutral)
- [ ] Success (green accent)
- [ ] Error (red accent)
- [ ] Warning (amber accent)
- [ ] Info (blue accent)
- [ ] Loading (with indeterminate progress indicator)

### Visual Design

- [ ] Material 3 color scheme integration (uses theme colors by default)
- [ ] Dynamic color support (Material You)
- [ ] Dark mode support (automatic)
- [ ] Rounded card appearance with elevation/shadow
- [ ] Leading icon per toast type (configurable)
- [ ] Title + description layout (single line or two lines)

### Theming & Customization

- [ ] `ToastStackDefaults` object with sensible defaults
- [ ] `ToastStackStyle` data class for global style overrides
- [ ] Per-toast style override via `show()` parameters
- [ ] Custom background color, text color, border, shape
- [ ] Custom icon composable slot
- [ ] Custom font/typography override

---

## Phase 3 - Animations (v0.3.0)

### Entry Animations

- [ ] Slide in from top (for top-positioned toasts)
- [ ] Slide in from bottom (for bottom-positioned toasts)
- [ ] Fade in
- [ ] Scale + fade combination
- [ ] Configurable animation duration and easing

### Exit Animations

- [ ] Slide out (reverse of entry direction)
- [ ] Fade out
- [ ] Shrink + fade combination
- [ ] Height collapse (stack reflows smoothly when a toast is removed)

### Gestures

- [ ] Swipe left to dismiss
- [ ] Swipe right to dismiss
- [ ] Swipe direction configurable (left, right, both, none)
- [ ] Drag follow (toast follows finger during swipe)
- [ ] Opacity fade during swipe (visual feedback)
- [ ] Velocity-based dismiss (fast flick vs slow drag threshold)

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

### Progress Toasts

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
- [ ] Custom content still gets swipe-to-dismiss and auto-dismiss behavior

### Stacking Modes

- [ ] Expanded mode (all toasts visible, stacked vertically with spacing)
- [ ] Compact mode (newest toast fully visible, older ones peek behind with offset, like iOS notifications)
- [ ] Tap compact stack to expand
- [ ] Configurable max stack depth in compact mode

### Haptic Feedback

- [ ] Optional vibration on toast appearance
- [ ] Haptic feedback intensity per toast type (e.g., stronger for errors)
- [ ] Respects system haptic settings

### Accessibility

- [ ] Content descriptions announced via TalkBack
- [ ] Toast type announced (e.g., "Error notification")
- [ ] Action buttons focusable and activatable via accessibility services
- [ ] Pause auto-dismiss when TalkBack is active
- [ ] Sufficient color contrast for all built-in types
- [ ] Reduced motion support (simplified animations when system setting is on)

### Sound

- [ ] Optional notification sound on appearance
- [ ] Per-type sound customization
- [ ] Respects system Do Not Disturb and silent mode

---

## Phase 6 - Developer Experience (v1.0.0)

### API Surface

- [ ] `ToastStack.success("message")` - one-liner for each type
- [ ] `ToastStack.error("message")`
- [ ] `ToastStack.warning("message")`
- [ ] `ToastStack.info("message")`
- [ ] `ToastStack.loading("message")` returning an ID for later update
- [ ] `ToastStack.show { ... }` builder DSL for complex toasts
- [ ] `ToastStack.dismiss(id)` / `ToastStack.dismissAll()`
- [ ] `ToastStack.update(id) { ... }` to modify an active toast

### Configuration DSL

- [ ] Global configuration via `ToastStackHost(config = ToastStackConfig { ... })`
- [ ] Duration defaults per type
- [ ] Animation defaults
- [ ] Position default
- [ ] Style defaults
- [ ] Max visible count

### Testing Support

- [ ] `TestToastStackState` for unit testing toast emissions from ViewModels
- [ ] Assertion helpers: `assertToastShown(type, message)`, `assertNoToasts()`
- [ ] Compose UI test rule integration

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

## Non-Goals

These are explicitly out of scope to keep the library focused:

- Full notification system (use Android NotificationManager for that)
- Persistent notifications that survive process death
- Remote/push notification display
- Dialog replacement (toasts are ephemeral, not blocking)
- Snackbar compatibility layer (this is a replacement, not a wrapper)
