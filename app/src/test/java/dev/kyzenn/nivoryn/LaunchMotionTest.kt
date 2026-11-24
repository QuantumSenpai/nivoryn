package dev.kyzenn.nivoryn

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.kyzenn.nivoryn.ui.NivorynApp
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchMotionTest {
    @get:Rule val compose=createComposeRule()
    @Test fun usableHomeDoesNotWaitForAnArtificialSplashOrOnboarding() {
        compose.mainClock.autoAdvance=false
        compose.setContent { NivorynApp() }
        compose.onNodeWithText("Scan").assertExists()
        compose.onNodeWithText("Paste").assertExists()
        compose.onNodeWithText("Check").performClick()
        compose.mainClock.advanceTimeBy(16)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Review message").assertExists()
    }
    @Test fun incomingShareBypassesHomeAndPreservesOriginalContent() {
        val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Shared private content")
        compose.setContent { NivorynApp(intent) }
        compose.onNode(hasSetTextAction()).assertTextContains("Shared private content")
        compose.onNodeWithText("Scan").assertDoesNotExist()
    }
}
