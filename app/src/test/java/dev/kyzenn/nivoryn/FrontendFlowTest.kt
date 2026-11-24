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
    @Test fun settingsResourcesAndLiveUsageHaveConnectedDestinations() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNode(hasText("Models and resources") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("On-device engines").assertExists()
        compose.onNodeWithText("Llama resources").performScrollTo().performClick()
        compose.onNodeWithText("Set up local AI").assertExists()
        compose.onNode(hasText("Settings") and isSelectable()).assertIsSelected()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Rules and indicators").performScrollTo().performClick()
        compose.onNodeWithText("Offline resources · authenticated updates").assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Network activity").performScrollTo().performClick()
        compose.onNodeWithText("Start observation").assertExists()
        compose.onNodeWithText("Inspect network capture").performScrollTo().performClick()
        compose.onNodeWithText("Import PCAP").assertExists()
    }
    @Before fun setup() {
        // Each test starts from its own fresh activity/process test sandbox.
    }
    @Test fun sampleReviewResultAndBackKeepDraft() {
        compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
        compose.onNodeWithText("The urgent KYC message").performScrollTo().performClick()
        compose.onNodeWithText("Show example result").performScrollTo().performClick()
        compose.onNodeWithText("!  High risk").assertExists()
        compose.onNodeWithText("Example result · Authored demo data").assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Editable text").assertExists()
        compose.onNodeWithText("Show example result").assertExists()
    }
    @Test fun changingSampleNeverLabelsCustomContentHighRisk() {
        compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
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
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Keep this draft")
        compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
        compose.onNodeWithText("The urgent KYC message").performScrollTo().performClick()
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Keep this draft")
    }
    @Test fun caseCreateRenameAndDeleteAreConnected() {
        compose.onNodeWithText("Check").performClick()
        compose.onNodeWithText("Activity").performClick()
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
