package dev.kyzenn.nivoryn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.ui.HomeScreen
import dev.kyzenn.nivoryn.ui.theme.NivorynTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InputUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun oneChooserKeepsSelectionImportsAndReviewConnected() {
        var imported = ""
        var reviewed = false
        compose.setContent {
            var draft by remember { mutableStateOf(CheckDraft()) }
            NivorynTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    HomeScreen(draft, false, { draft = draft.copy(text = it) }, { reviewed = true },
                        { imported = it }, {}, {}, { draft = draft.copy(kind = it) })
                }
            }
        }
        compose.onAllNodesWithText("Link").assertCountEquals(1)
        compose.onNodeWithText("Link").performClick()
        compose.onNode(hasText("Link") and hasClickAction()).assert(
            SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Selected"))
        compose.onNodeWithText("Screenshot").performClick()
        compose.runOnIdle { Assert.assertEquals("Screenshot", imported) }
        compose.onNodeWithText("APK").assertDoesNotExist()
        compose.onNodeWithText("More import options").performScrollTo().performClick()
        compose.onNodeWithText("APK").performScrollTo().performClick()
        compose.runOnIdle { Assert.assertEquals("APK", imported) }
        compose.onNode(hasSetTextAction()).performTextInput("Check this message")
        compose.onNodeWithText("Review input").performScrollTo().performClick()
        compose.runOnIdle { Assert.assertTrue(reviewed) }
    }
    @Test fun optionalDetailsStayAvailableWithoutCrowdingTheNextAction() {
        val draft = CheckDraft(text = "Share your OTP now.\n".repeat(8))
        val result = dev.kyzenn.nivoryn.domain.AnalyseInput().analyse(draft)
        compose.setContent {
            NivorynTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    dev.kyzenn.nivoryn.ui.ResultScreen(result, false, {}, {}, {}, draft)
                }
            }
        }
        compose.onNodeWithText("What to do next").assertExists()
        val detail = "Completed · ${result.completed.first()}"
        compose.onNodeWithText(detail).assertDoesNotExist()
        compose.onNodeWithText("View check details").performScrollTo().performClick()
        compose.onNodeWithText(detail).assertExists()
        compose.onNodeWithText("Show full input").performScrollTo().performClick()
        compose.onNodeWithText("Show less input").assertExists()
    }

}
