package dev.kyzenn.nivoryn
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w390dp-h844dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeRenderTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private fun capture(name:String) {
  compose.waitForIdle()
  compose.runOnUiThread {
   val view=compose.activity.window.decorView
   val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
   view.draw(Canvas(bitmap))
   val dir=File("build/frontend-screenshots");dir.mkdirs()
   File(dir,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
  }
 }
 @Test @Config(qualifiers="w1024dp-h768dp-land-mdpi") fun renderTablet() {
  compose.onNodeWithText("Check").performClick()
  compose.onNodeWithText("Activity").assertExists()
  capture("tablet-home")
 }
 @Test @Config(qualifiers="w390dp-h844dp-port-mdpi") fun renderDarkAndHindi() {
  compose.onNodeWithText("Check").performClick()
  compose.onNodeWithText("Settings").performClick()
  compose.onNodeWithContentDescription("Dark").performClick()
  compose.onNodeWithText("Home").performClick()
  capture("dark-home")
  compose.onNodeWithText("हिन्दी").performClick()
  compose.onNodeWithText("क्या जाँचना है?").assertExists()
  capture("hindi-home")
 }
 @Test fun renderWelcomeHomeAndResult() {
  capture("welcome")
  compose.onNodeWithText("Check").performClick()
  capture("home")
  compose.onNodeWithText("Explore sample checks · example data").performScrollTo().performClick()
  compose.onNodeWithText("Pay to unlock earnings").performScrollTo().performClick()
  compose.onNodeWithText("Show example result").performScrollTo().performClick()
  compose.waitUntil(5000) { compose.onAllNodesWithText("!  High risk").fetchSemanticsNodes().size == 1 }
  compose.onNodeWithText("!  High risk").assertExists()
  compose.onNodeWithText("View evidence").performClick()
  capture("result")
  compose.onNodeWithText("Check again").performScrollTo().performClick()
  compose.onNodeWithText("Editable text").performScrollTo()
  capture("input")
 }
 @Test fun renderRealOfflineResult() {
  compose.onNodeWithText("Check").performClick()
  compose.onNode(hasSetTextAction()).performTextInput("Please send your OTP to finish verification.")
  compose.onNodeWithText("Review message").performScrollTo().performClick()
  compose.onNodeWithText("Run local check").performScrollTo().performClick()
  compose.waitUntil(5000) { compose.onAllNodesWithText("!  High risk").fetchSemanticsNodes().size == 1 }
  compose.onNodeWithText("View evidence").performClick()
  capture("local-result")
 }
}
