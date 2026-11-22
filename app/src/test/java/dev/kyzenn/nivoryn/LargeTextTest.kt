package dev.kyzenn.nivoryn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.kyzenn.nivoryn.ui.NivorynApp
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeTextTest {
 @get:Rule val compose=createComposeRule()
 @Test fun largeTypeKeepsPrimaryActionsReachableAndTextEditable() {
  compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f,1.6f)) { NivorynApp() } }
  compose.onNodeWithText("Start checking").performScrollTo().assertIsDisplayed().performClick()
  compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("A long filename or Hindi message · नमस्ते")
  compose.onNodeWithText("Review input").performScrollTo().assertIsDisplayed().performClick()
  compose.onNodeWithText("Run local check").performScrollTo().assertIsDisplayed()
 }
}
