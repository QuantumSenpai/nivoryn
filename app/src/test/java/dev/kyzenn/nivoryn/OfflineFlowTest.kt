package dev.kyzenn.nivoryn

import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfflineFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun realCheckUsesOriginalEvidenceAndEditingClearsOldAssessment() {
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Please send your OTP to finish verification.")
        compose.onNodeWithText("Review message").performScrollTo().performClick()
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("!  High risk").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("!  High risk").assertExists()
        compose.onNodeWithText("Local check · Evidence-led; signals are not a verdict").assertExists()
        compose.onNodeWithText("View evidence").performScrollTo().performClick()
        compose.onNodeWithText("A secret is requested").assertExists()
        compose.onNodeWithText("“send your OTP”", substring=true).assertExists()
        compose.onNodeWithText("Save check locally").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Check again").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Our community meeting starts at six pm.")
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("!  No clear warning found").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("!  No clear warning found").assertExists()
        compose.onNodeWithText("!  High risk").assertDoesNotExist()
    }
    @Test fun supportedSetupPermissionsDoNotEnableSensitiveDataAccess() {
        val activity = compose.activity
        val permissions = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        Assert.assertTrue(permissions.contains("android.permission.INTERNET"))
        Assert.assertFalse(permissions.contains("android.permission.READ_SMS"))
        Assert.assertFalse(permissions.contains("android.permission.READ_CALL_LOG"))
        Assert.assertTrue(permissions.contains("android.permission.ACCESS_NETWORK_STATE"))
        Assert.assertFalse(activity.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }
    @Test fun leavingACheckDoesNotNavigateBackToALateResult() {
        compose.onNodeWithText("Check").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Send your OTP. " + "Ordinary context. ".repeat(600))
        compose.onNodeWithText("Review message").performScrollTo().performClick()
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.onNodeWithText("Back").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Review your input").fetchSemanticsNodes().size == 1 }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.onNodeWithText("Review your input").assertExists()
        compose.onNodeWithText("Local check · Evidence-led; signals are not a verdict").assertDoesNotExist()
    }
    @Test fun dashboardLinkSelectionOwnsTheActualInputMode() {
        compose.onNodeWithText("Check").performClick()
        compose.onAllNodesWithText("Link").onFirst().performClick()
        compose.onNode(hasSetTextAction()).performTextInput("javascript:alert(1)")
        compose.onNodeWithText("Check link").performScrollTo().performClick()
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("!  Cannot assess").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("!  No clear warning found").assertDoesNotExist()
    }
}
