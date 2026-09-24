package com.siliconcircuits.toaststack

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that `ToastStack.configure(...)` reaches the host that
 * [ToastStackInitializer] adds to every Activity: deduplication with the
 * repeat count on the card, and a default duration set after the overlay
 * was created.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutoOverlayConfigTest {

    // Configure and start the auto initializer before the rule launches the
    // Activity, the same order as Application.onCreate() in a real app.
    private val autoInit = object : ExternalResource() {
        override fun before() {
            ToastStack.configure(deduplicationWindowMs = 60_000)
            ToastStackInitializer().create(ApplicationProvider.getApplicationContext())
        }
    }

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(autoInit).around(composeTestRule)

    @After
    fun tearDown() {
        ToastStack.dismissAll()
        ToastStack.configure(deduplicationWindowMs = 0L, defaultDuration = ToastDuration.Short)
    }

    @Test
    fun `global host shows one card with count 5 for the same message 5 times`() {
        composeTestRule.waitForIdle()
        val ids = composeTestRule.runOnUiThread {
            (1..5).map { ToastStack.show("Job failed")?.id }
        }

        assertEquals(1, ids.toSet().size)
        composeTestRule.onNodeWithText("Job failed (x5)").assertExists()
    }

    @Test
    fun `configure after the overlay is created reaches the next toast`() {
        composeTestRule.waitForIdle()
        val duration = composeTestRule.runOnUiThread {
            ToastStack.configure(defaultDuration = ToastDuration.Custom(8_000))
            ToastStack.warning("Later")
            ToastStack.resolveHost(null)?.toasts?.single { it.message == "Later" }?.duration?.millis
        }

        assertEquals(8_000L, duration)
    }
}
