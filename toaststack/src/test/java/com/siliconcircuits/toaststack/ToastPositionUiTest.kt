package com.siliconcircuits.toaststack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks which side the Start and End cards sit on in each layout direction. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class ToastPositionUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val corners = listOf(
        ToastPosition.TopStart, ToastPosition.TopEnd, ToastPosition.BottomStart, ToastPosition.BottomEnd
    )

    /** Shows one card per corner and returns each card's center x as a share of the screen width. */
    private fun cardCenters(direction: LayoutDirection): Map<ToastPosition, Float> {
        val state = ToastStackState()
        var width = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                ToastStackHost(state = state, tag = "position", colorSource = ToastColorSource.Library)
            }
        }
        composeTestRule.runOnIdle {
            corners.forEach { state.show(it.name, position = it, duration = ToastDuration.Indefinite) }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        return corners.associateWith { position ->
            val node = composeTestRule.onNode(hasContentDescription(position.name, substring = true)).fetchSemanticsNode()
            var root = node.layoutInfo.coordinates
            while (root.parentLayoutCoordinates != null) root = root.parentLayoutCoordinates!!
            width = root.size.width
            node.boundsInRoot.center.x / width
        }
    }

    @Test
    fun `in LTR Start cards sit on the left and End cards on the right`() {
        val centers = cardCenters(LayoutDirection.Ltr)

        assertTrue(centers.toString(), centers.getValue(ToastPosition.TopStart) < 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.BottomStart) < 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.TopEnd) > 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.BottomEnd) > 0.5f)
    }

    @Test
    fun `in RTL Start cards sit on the right and End cards on the left`() {
        val centers = cardCenters(LayoutDirection.Rtl)

        assertTrue(centers.toString(), centers.getValue(ToastPosition.TopStart) > 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.BottomStart) > 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.TopEnd) < 0.5f)
        assertTrue(centers.toString(), centers.getValue(ToastPosition.BottomEnd) < 0.5f)
    }
}
