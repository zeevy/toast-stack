package com.siliconcircuits.toaststack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Checks where the cards sit on screen, and where the icon and the text sit in a card. */
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

    /** Shows [show] on a host at [fontScale] in [direction] and waits for the enter animation. */
    private fun showToast(fontScale: Float = 1f, direction: LayoutDirection = LayoutDirection.Ltr, show: (ToastStackState) -> Unit) {
        val state = ToastStackState()
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
                LocalLayoutDirection provides direction,
            ) {
                ToastStackHost(state = state, tag = "layout", colorSource = ToastColorSource.Library)
            }
        }
        composeTestRule.runOnIdle { show(state) }
        composeTestRule.mainClock.advanceTimeBy(1_000)
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(results)
        return results.single()
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the icon is centred on the title line and the message starts at the icon's left edge`() {
        showToast(fontScale = 1.3f) { it.success("Your changes were saved", title = "Saved", duration = ToastDuration.Indefinite) }

        val icon = composeTestRule.onNodeWithTag("toaststack_type_icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Exact text match: the slot's stand-in character is not read out.
        val titleNode = composeTestRule.onNodeWithText("Saved")
        val title = titleNode.fetchSemanticsNode().boundsInRoot
        val titleLine = titleNode.textLayout()
        val message = composeTestRule.onNodeWithText("Your changes were saved").fetchSemanticsNode().boundsInRoot

        val lineCentre = title.top + (titleLine.getLineTop(0) + titleLine.getLineBottom(0)) / 2
        assertEquals("icon centre $icon, title line centre $lineCentre", lineCentre, icon.center.y, 1f)
        assertEquals("icon $icon, message $message", icon.left, message.left, 1f)
        assertTrue("icon $icon, message $message", icon.bottom <= message.top)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a wrapped message with no title starts its second line at the left edge`() {
        showToast {
            it.warning("Only 120 MB of storage left. Delete some photos or videos to free up space.", duration = ToastDuration.Indefinite)
        }

        val icon = composeTestRule.onNodeWithTag("toaststack_type_icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val messageNode = composeTestRule.onNodeWithText("Only 120 MB of storage left. Delete some photos or videos to free up space.")
        val message = messageNode.fetchSemanticsNode().boundsInRoot
        val layout = messageNode.textLayout()

        assertTrue("lines ${layout.lineCount}", layout.lineCount >= 2)
        assertEquals("icon $icon, message $message", icon.left, message.left, 1f)
        assertEquals(0f, layout.getLineLeft(1), 1f)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `when the text direction is not the layout direction the gap stays between the icon and the text`() {
        // An RTL app whose text follows its content, showing English text.
        val style = ToastStackStyle(messageStyle = TextStyle(textDirection = TextDirection.Content))
        showToast(direction = LayoutDirection.Rtl) {
            it.show("Wi-Fi connected", type = ToastType.Success, style = style, duration = ToastDuration.Indefinite)
        }

        val icon = composeTestRule.onNodeWithTag("toaststack_type_icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val message = composeTestRule.onNodeWithText("Wi-Fi connected").fetchSemanticsNode().boundsInRoot

        // English reads left to right, so the icon is at the left end of the text, with no gap before it.
        assertEquals("icon $icon, message $message", message.left, icon.left, 1f)
    }
}
