package dev.kyzenn.nivoryn

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class FrontendFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun settingsOnlyOffersConnectedEngineScreens() {
        compose.onNodeWithText("Start checking").performScrollTo().performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Live network monitor").assertDoesNotExist()
        listOf("Rules and indicators" to "Offline resources · authenticated updates",
            "Llama resources" to "Local model",
            "Network replay" to "Import PCAP",
            "APK indicators" to "Import APK").forEach { (label, destination) ->
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.onNodeWithText(destination).assertExists()
            compose.onNodeWithText("Back").performClick()
            compose.onNodeWithText("Make yourself at home").assertExists()
        }
    }
    @Before fun setup() {
        // Each test starts from its own fresh activity/process test sandbox.
    }
    @Test fun sampleReviewResultAndBackKeepDraft() {
        compose.onNodeWithText("Try a sample").performScrollTo().performClick()
        compose.onNodeWithText("The urgent KYC message").performScrollTo().performClick()
        compose.onNodeWithText("Show example result").performScrollTo().performClick()
        compose.onNodeWithText("!  High risk").assertExists()
        compose.onNodeWithText("Example result · Authored demo data").assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Editable text").assertExists()
        compose.onNodeWithText("Show example result").assertExists()
    }
    @Test fun changingSampleNeverLabelsCustomContentHighRisk() {
        compose.onNodeWithText("Try a sample").performScrollTo().performClick()
        compose.onNodeWithText("The urgent KYC message").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("My own private message")
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Local check · Evidence-led; signals are not a verdict").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("!  No clear warning found").assertExists()
        compose.onNodeWithText("Local check · Evidence-led; signals are not a verdict").assertExists()
        compose.onNodeWithText("Example result · Authored demo data").assertDoesNotExist()
        compose.onNodeWithText("!  High risk").assertDoesNotExist()
        compose.onNodeWithText("Save example to a case").assertDoesNotExist()
    }
    @Test fun exploringExamplesKeepsTheOriginalCustomDraft() {
        compose.onNodeWithText("Start checking").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Keep this draft")
        compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
        compose.onNodeWithText("The urgent KYC message").performScrollTo().performClick()
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Keep this draft")
    }
    @Test fun caseCreateRenameAndDeleteAreConnected() {
        compose.onNodeWithText("Start checking").performScrollTo().performClick()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("Create a demo case").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("My demo case")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("My demo case").performClick()
        compose.onNodeWithText("Rename").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Renamed case")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Renamed case").assertExists()
        compose.onNodeWithText("Delete case").performClick()
        compose.onNodeWithText("Delete Renamed case?").assertExists()
        compose.onAllNodesWithText("Delete case").onLast().performClick()
        compose.onNodeWithText("No saved cases yet").assertExists()
    }
}
