package dev.kyzenn.nivoryn
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import dev.kyzenn.nivoryn.ui.NivorynApp
import dev.kyzenn.nivoryn.data.FrontendStore
import dev.kyzenn.nivoryn.domain.DemoCase
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class RestorationTest {
 @get:Rule val compose=createComposeRule()
 @Test fun changingInputModeCannotReuseAnOldQrOrAttachment() {
  val draft=dev.kyzenn.nivoryn.domain.CheckDraft(text="Reviewed text",attachmentUri="content://scan/old",filename="old.jpg",mime="image/jpeg",bytes=100,kind="QR",qrPayload="https://old.example",extractionSource="QR decoder")
  val next=draft.forInputMode("Text")
  Assert.assertEquals(dev.kyzenn.nivoryn.domain.CheckDraft(text="Reviewed text",kind="Text"),next)
  Assert.assertEquals(draft,draft.forInputMode("QR"))
  Assert.assertEquals(next,next.copy(qrPayload="stale").forInputMode("Text"))
 }
 @Test fun recreationKeepsCustomDraftAndReviewRoute() {
  val restoration=StateRestorationTester(compose)
  restoration.setContent{NivorynApp()}
  compose.onNodeWithText("Check").performClick()
  compose.onNode(hasSetTextAction()).performTextInput("My draft · नमस्ते")
  compose.onNodeWithText("Review message").performScrollTo().performClick()
  restoration.emulateSavedInstanceStateRestore()
  compose.onNodeWithText("Review your input").assertExists()
  compose.onNode(hasSetTextAction()).assertTextContains("My draft · नमस्ते")
 }
 @Test fun textSharesRemainCustomAndBounded() {
  val intent=android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
    .putExtra(android.content.Intent.EXTRA_TEXT,android.text.SpannedString("hello".repeat(3000)))
  val text=dev.kyzenn.nivoryn.integration.ShareInputParser.text(intent)
  Assert.assertEquals(12000,text.length)
  Assert.assertNull(dev.kyzenn.nivoryn.data.FrontendDemoRepository().resultFor(dev.kyzenn.nivoryn.domain.CheckDraft(text=text)))
 }
 @Test fun casesRoundTripThroughAppPrivateStorage() {
  val store=FrontendStore(ApplicationProvider.getApplicationContext())
  val cases=listOf(DemoCase("a","A local case",listOf("task","kyc")))
  store.saveCases(cases)
  Assert.assertEquals(cases,FrontendStore(ApplicationProvider.getApplicationContext()).cases())
  store.saveCases(emptyList())
  Assert.assertTrue(store.cases().isEmpty())
 }
}
