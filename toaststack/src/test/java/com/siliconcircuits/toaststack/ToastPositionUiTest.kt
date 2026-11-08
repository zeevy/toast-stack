package com.siliconcircuits.toaststack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.geometry.Rect
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

    private fun bounds(tag: String) =
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    /** The line lays out 0 dp high (a hairline draws one pixel), so its bounds are empty: read its place instead. */
    private fun lineBounds(): Rect {
        val node = composeTestRule.onNodeWithTag("toaststack_divider", useUnmergedTree = true).fetchSemanticsNode()
        val topLeft = node.positionInRoot
        return Rect(topLeft.x, topLeft.y, topLeft.x + node.size.width, topLeft.y + node.size.height)
    }

    private fun textBounds(text: String) = composeTestRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `with a title the icon has its own column and the message goes below a line at full width`() {
        showToast { it.success("All 3 files are on the server and shared with your team.", title = "Upload complete", duration = ToastDuration.Indefinite) }

        val icon = bounds("toaststack_type_icon")
        val title = textBounds("Upload complete")
        val line = lineBounds()
        val message = textBounds("All 3 files are on the server and shared with your team.")

        assertTrue("icon $icon, title $title", icon.right <= title.left)
        assertEquals("icon $icon, message $message", icon.left, message.left, 1f)
        assertTrue("line $line, message $message", line.bottom <= message.top)
        assertTrue("title $title, line $line", title.bottom <= line.top)
        assertEquals("line $line, icon $icon", icon.left, line.left, 1f)
        assertTrue("line $line, message $message", line.right >= message.right)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a header of two lines keeps the icon centred on it at font scale 1_3`() {
        showToast(fontScale = 1.3f) {
            it.error("Check your network and try again.", title = "Connection timed out while sending your report", duration = ToastDuration.Indefinite)
        }

        val icon = bounds("toaststack_type_icon")
        val titleNode = composeTestRule.onNodeWithText("Connection timed out while sending your report")
        val title = titleNode.fetchSemanticsNode().boundsInRoot

        assertTrue("lines ${titleNode.textLayout().lineCount}", titleNode.textLayout().lineCount >= 2)
        assertEquals("icon $icon, title $title", title.center.y, icon.center.y, 1f)
        assertTrue("icon $icon, title $title", icon.right <= title.left)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a wrapped message with no title stays right of the icon and has no line`() {
        val text = "Only 120 MB of storage left. Delete some photos or videos to free up space."
        showToast { it.warning(text, duration = ToastDuration.Indefinite) }

        val icon = bounds("toaststack_type_icon")
        val messageNode = composeTestRule.onNodeWithText(text)
        val message = messageNode.fetchSemanticsNode().boundsInRoot

        assertTrue("lines ${messageNode.textLayout().lineCount}", messageNode.textLayout().lineCount >= 2)
        assertTrue("icon $icon, message $message", icon.right <= message.left)
        composeTestRule.onAllNodesWithTag("toaststack_divider", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `a title with an empty message has no line`() {
        showToast { it.info("", title = "Synced", duration = ToastDuration.Indefinite) }

        composeTestRule.onNodeWithText("Synced").assertExists()
        composeTestRule.onAllNodesWithTag("toaststack_divider", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `a title with a blank message has no line, and a repeated description keeps its count`() {
        val state = ToastStackState(deduplicationWindowMs = 5_000)
        composeTestRule.setContent { ToastStackHost(state = state, tag = "layout", colorSource = ToastColorSource.Library) }
        composeTestRule.runOnIdle {
            state.info("  ", title = "Blank", duration = ToastDuration.Indefinite)
            repeat(2) { state.info("Saved", title = "Repeated", duration = ToastDuration.Indefinite) }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)

        // Only the repeated toast has a description, with its repeat count in it.
        composeTestRule.onAllNodesWithTag("toaststack_divider", useUnmergedTree = true).assertCountEquals(1)
        composeTestRule.onNodeWithText("Saved (x2)").assertExists()
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `with no icon the header and the description start at the same edge`() {
        showToast { it.show("You can find it under Drafts.", title = "Draft saved", duration = ToastDuration.Indefinite) }

        val title = textBounds("Draft saved")
        val message = textBounds("You can find it under Drafts.")

        assertEquals("title $title, message $message", title.left, message.left, 1f)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `actions and the close button sit in the header and the description runs under them`() {
        showToast {
            it.show(
                "The note Groceries was moved to the bin.", title = "Item deleted",
                showCloseButton = true, duration = ToastDuration.Indefinite
            ).withAction("Undo") {}
        }

        val title = textBounds("Item deleted")
        val undo = textBounds("Undo")
        val close = composeTestRule.onNode(hasContentDescription("Dismiss toast")).fetchSemanticsNode().boundsInRoot
        val line = lineBounds()

        assertTrue("undo $undo, title $title", undo.top < title.bottom && undo.bottom > title.top)
        assertTrue("close $close, title $title", close.top < title.bottom && close.bottom > title.top)
        assertTrue("undo $undo, line $line", line.top >= undo.bottom || line.top >= close.bottom)
        assertTrue("line $line reaches under undo $undo", line.right > undo.left)
    }

    // Native graphics, so text and icons are measured for real.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `in RTL the icon column is on the right and the description starts at the right edge`() {
        showToast(direction = LayoutDirection.Rtl) { it.info("Tap to install it now.", title = "A new update is available", duration = ToastDuration.Indefinite) }

        val icon = bounds("toaststack_type_icon")
        val title = textBounds("A new update is available")
        val message = textBounds("Tap to install it now.")

        assertTrue("icon $icon, title $title", icon.left >= title.right)
        assertEquals("icon $icon, message $message", icon.right, message.right, 1f)
    }
}
