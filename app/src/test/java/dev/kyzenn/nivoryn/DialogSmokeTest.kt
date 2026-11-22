package dev.kyzenn.nivoryn
import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.ui.components.*
import dev.kyzenn.nivoryn.ui.theme.*
import org.robolectric.annotation.GraphicsMode
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class DialogSmokeTest {
 @get:Rule val compose=createComposeRule()
 @Before fun timeout() { androidx.test.espresso.IdlingPolicies.setMasterPolicyTimeout(5,java.util.concurrent.TimeUnit.SECONDS) }
 @Test fun editDialog() {
  compose.setContent { NivorynTheme { var text by remember{mutableStateOf("")};AlertDialog(onDismissRequest={},title={Text("Title")},text={FormField(text,"Case name",{text=it})},confirmButton={TextButton({}){Text("Save")}}) } }
  compose.onNode(hasSetTextAction()).performTextInput("test")
  compose.onNodeWithText("test").assertExists()
 }
}
