package com.siliconcircuits.toaststack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Compose UI tests that verify toast enter and exit animations by
 * controlling the test clock. Runs on the JVM via Robolectric so no
 * device or emulator is needed.
 *
 * The test clock controls animation progress: `mainClock.advanceTimeBy`
 * moves the animation forward by the given milliseconds, letting us
 * verify that toasts appear, remain visible mid animation, and
 * disappear after the exit completes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToastAnimationUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- Enter animations: each type should make the toast visible ---

    @Test
    fun `slide enter makes toast visible`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Slide enter", animation = ToastAnimation.Slide)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Slide enter").assertIsDisplayed()
    }

    @Test
    fun `fade enter makes toast visible`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Fade enter", animation = ToastAnimation.Fade)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Fade enter").assertIsDisplayed()
    }

    @Test
    fun `scaleAndFade enter makes toast visible`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Scale enter", animation = ToastAnimation.ScaleAndFade)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Scale enter").assertIsDisplayed()
    }

    // --- Exit animations: each type should remove the toast after dismiss ---

    @Test
    fun `slide exit removes toast after dismiss`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        val handle = state.show(
            "Slide exit",
            animation = ToastAnimation.Slide,
            duration = ToastDuration.Indefinite
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Slide exit").assertIsDisplayed()

        state.dismiss(handle.id)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Slide exit").assertDoesNotExist()
    }

    @Test
    fun `fade exit removes toast after dismiss`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        val handle = state.show(
            "Fade exit",
            animation = ToastAnimation.Fade,
            duration = ToastDuration.Indefinite
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Fade exit").assertIsDisplayed()

        state.dismiss(handle.id)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Fade exit").assertDoesNotExist()
    }

    @Test
    fun `scaleAndFade exit removes toast after dismiss`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        val handle = state.show(
            "Scale exit",
            animation = ToastAnimation.ScaleAndFade,
            duration = ToastDuration.Indefinite
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Scale exit").assertIsDisplayed()

        state.dismiss(handle.id)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Scale exit").assertDoesNotExist()
    }

    // --- Custom timing ---

    @Test
    fun `custom slow enter is visible after full duration`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "Slow toast",
            animation = ToastAnimation.Slide,
            animationConfig = ToastAnimationConfig(enterDurationMillis = 800)
        )

        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithText("Slow toast").assertIsDisplayed()
    }

    // --- Programmatic dismiss removes toast ---

    @Test
    fun `programmatic dismissAll removes all toasts`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Toast A", duration = ToastDuration.Indefinite)
        state.show("Toast B", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Toast A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toast B").assertIsDisplayed()

        state.dismissAll()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Toast A").assertDoesNotExist()
        composeTestRule.onNodeWithText("Toast B").assertDoesNotExist()
    }

    // --- Close button ---

    @Test
    fun `close button triggers exit animation`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "Close me",
            duration = ToastDuration.Indefinite,
            showCloseButton = true
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Close me").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Dismiss toast").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Close me").assertDoesNotExist()
    }

    // --- Stack reflow ---

    @Test
    fun `dismiss middle toast keeps others visible`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("First", duration = ToastDuration.Indefinite)
        val middleHandle = state.show("Middle", duration = ToastDuration.Indefinite)
        state.show("Third", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Middle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third").assertIsDisplayed()

        state.dismiss(middleHandle.id)
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Middle").assertDoesNotExist()
        composeTestRule.onNodeWithText("Third").assertIsDisplayed()
    }

    // --- Dismiss all ---

    @Test
    fun `dismissAll removes all toasts after exit animation`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("A", duration = ToastDuration.Indefinite)
        state.show("B", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()

        state.dismissAll()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
        composeTestRule.onNodeWithText("B").assertDoesNotExist()
    }

    // --- Title + message rendering ---

    @Test
    fun `toast with title shows both title and message`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Body text", title = "Headline", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Headline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Body text").assertIsDisplayed()
    }

    @Test
    fun `toast without title shows only message`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Message only", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("Message only").assertIsDisplayed()
    }

    // --- Close button visibility ---

    @Test
    fun `close button is not shown when showCloseButton is false`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "No close",
            duration = ToastDuration.Indefinite,
            showCloseButton = false
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("No close").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss toast").assertDoesNotExist()
    }

    @Test
    fun `close button is shown when showCloseButton is true`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "With close",
            duration = ToastDuration.Indefinite,
            showCloseButton = true
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithContentDescription("Dismiss toast").assertIsDisplayed()
    }

    // --- Accessibility: content description on toast ---

    @Test
    fun `toast has content description matching message`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Accessible toast", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNode(hasContentDescription("Accessible toast")).assertExists()
    }

    @Test
    fun `toast with title has combined content description`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "Details here",
            title = "Alert",
            duration = ToastDuration.Indefinite
        )
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNode(hasContentDescription("Alert. Details here")).assertExists()
    }

    // --- Type announced in the card label; the icon is decorative ---

    @Test
    fun `each typed toast announces its type and its icon has no label`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.success("Done", duration = ToastDuration.Indefinite)
        state.error("Failed", duration = ToastDuration.Indefinite)
        state.warning("Watch out", duration = ToastDuration.Indefinite)
        state.info("FYI", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNode(hasContentDescription("Success notification: Done")).assertExists()
        composeTestRule.onNode(hasContentDescription("Error notification: Failed")).assertExists()
        composeTestRule.onNode(hasContentDescription("Warning notification: Watch out")).assertExists()
        composeTestRule.onNode(hasContentDescription("Information notification: FYI")).assertExists()
        composeTestRule.onNode(hasContentDescription("icon", substring = true)).assertDoesNotExist()
    }

    // --- Close button fires CloseButton reason ---

    @Test
    fun `close button fires onDismiss with CloseButton reason`() {
        lateinit var state: ToastStackState
        var capturedReason: DismissReason? = null
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show(
            "Closeable",
            duration = ToastDuration.Indefinite,
            showCloseButton = true,
            onDismiss = { capturedReason = it }
        )
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithContentDescription("Dismiss toast").performClick()
        composeTestRule.mainClock.advanceTimeBy(500)

        assertEquals(DismissReason.CloseButton, capturedReason)
    }

    // --- Multiple types displayed simultaneously ---

    @Test
    fun `multiple toast types can be shown at once`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.success("Win", duration = ToastDuration.Indefinite)
        state.error("Fail", duration = ToastDuration.Indefinite)
        state.info("Note", duration = ToastDuration.Indefinite)
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onNodeWithText("Win").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Note").assertIsDisplayed()
    }

    // --- Max visible enforcement in UI ---

    @Test
    fun `max visible enforced in UI`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState(maxVisible = 2)
                ToastStackHost(state = state)
            }
        }

        state.show("First", duration = ToastDuration.Indefinite)
        state.show("Second", duration = ToastDuration.Indefinite)
        state.show("Third", duration = ToastDuration.Indefinite) // Queued
        composeTestRule.mainClock.advanceTimeBy(500)

        // Only First and Second visible. Third is queued, not shown yet.
        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third").assertDoesNotExist()
    }

    // --- Screen reader ---

    @Test
    fun `with TalkBack on before show, the toast stays and has a Dismiss action`() {
        val manager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(true)
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            MaterialTheme {
                state = rememberToastStackState()
                ToastStackHost(state = state)
            }
        }

        state.show("Read me", duration = ToastDuration.Short)
        composeTestRule.mainClock.advanceTimeBy(10_000)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
        composeTestRule.onNodeWithText("Read me").assertIsDisplayed()

        val dismiss = composeTestRule.onNodeWithContentDescription("Read me", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Dismiss" }
        composeTestRule.runOnIdle { dismiss.action() }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.onNodeWithText("Read me").assertDoesNotExist()
    }

    // --- Haptic ---

    @Test
    @Config(sdk = [24, 34])
    fun `a haptic toast plays the view haptic for its type`() {
        lateinit var state: ToastStackState
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            state = rememberToastStackState()
            ToastStackHost(state = state)
        }

        state.show("Saved", type = ToastType.Error, hapticEnabled = true)
        composeTestRule.waitForIdle()

        val expected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        assertEquals(expected, shadowOf(view).lastHapticFeedbackPerformed())
    }

    @Test
    fun `the library asks for no VIBRATE permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)

        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.VIBRATE))
    }

    // --- Translatable labels ---

    @Test
    @Config(qualifiers = "zz")
    fun `an app can translate the labels and the repeat count`() {
        lateinit var state: ToastStackState
        composeTestRule.setContent {
            state = rememberToastStackState(deduplicationWindowMs = 60_000)
            ToastStackHost(state = state)
        }

        composeTestRule.runOnIdle {
            repeat(3) { state.show("Upload failed", type = ToastType.Error, showCloseButton = true) }
        }

        composeTestRule.onNode(hasContentDescription("ZZ error: Upload failed [3 times]")).assertExists()
        composeTestRule.onNode(hasContentDescription("ZZ close")).assertExists()
    }
}
