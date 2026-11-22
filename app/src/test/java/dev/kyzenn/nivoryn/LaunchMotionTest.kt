package dev.kyzenn.nivoryn

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import dev.kyzenn.nivoryn.ui.SplashScreen
import dev.kyzenn.nivoryn.ui.theme.NivorynTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchMotionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun introCompletesOnceWithoutWaitingForResourcesOrRestarting() {
        var completions = 0
        var revision by mutableIntStateOf(0)
        compose.mainClock.autoAdvance = false
        compose.setContent { NivorynTheme { Text("Revision $revision"); SplashScreen { completions++ } } }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { Assert.assertEquals("Intro should have a readable entrance", 0, completions) }
        compose.mainClock.advanceTimeBy(450)
        compose.runOnIdle { Assert.assertEquals(1, completions); revision++ }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { Assert.assertEquals(1, completions) }
    }
    @Test fun reducedMotionEntersWithoutAForcedIntroDelay() {
        var completions = 0
        compose.setContent { NivorynTheme(reducedMotion = true) { SplashScreen { completions++ } } }
        compose.runOnIdle { Assert.assertEquals(1, completions) }
    }
}
