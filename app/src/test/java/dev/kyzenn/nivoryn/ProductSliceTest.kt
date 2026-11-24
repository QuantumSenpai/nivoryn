package dev.kyzenn.nivoryn

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProductSliceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun paymentPhraseSurvivesFromInputToEvidenceAndBack() {
        compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
        compose.onNodeWithText("Pay to unlock earnings").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Pay a fee to release your earnings.",substring=true)
        compose.onNodeWithText("Show example result").performScrollTo()
        val before=compose.onAllNodes(hasScrollAction()).onFirst().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        compose.onNodeWithText("Show example result").performClick()
        compose.onNodeWithText("Mode: authored example. No detection engine ran.").assertExists()
        compose.onNodeWithText("View evidence").performScrollTo().performClick()
        compose.onNodeWithText("Upfront payment pressure").assertExists()
        compose.onNodeWithText("Hide evidence").performClick()
        compose.onNodeWithText("Upfront payment pressure").assertDoesNotExist()
        compose.onNodeWithText("View evidence").performClick()
        compose.onNodeWithText("Original input").performScrollTo().assertExists()
        compose.onNodeWithText("Check again").performScrollTo().performClick()
        val after=compose.onAllNodes(hasScrollAction()).onFirst().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        Assert.assertEquals("Review scroll position survives returning from a result",before,after,1f)
        compose.onNode(hasSetTextAction()).assertTextContains("Pay a fee to release your earnings.",substring=true)
    }
    @Test fun fourRootDestinationsRetainCustomInput() {
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Retain this input")
        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Retain this input")
        compose.onNodeWithText("Home").assertExists()
        compose.onNodeWithText("Settings").assertExists()
    }
}
