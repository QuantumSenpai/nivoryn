package dev.kyzenn.nivoryn.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Easing

val LocalReducedMotion = compositionLocalOf { false }

private val Light = lightColorScheme(
    primary = Action,
    onPrimary = Color.White,
    secondary = LinkBlue,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    surfaceContainer = Color(0xFFF0F0EC),
    surfaceContainerHigh = Color(0xFFE6E6E1),
    surfaceContainerLow = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECECE8),
    onSurfaceVariant = Secondary,
    outline = Secondary,
    outlineVariant = Divider,
    error = Danger
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF7F7F5),
    onPrimary = Color(0xFF191919),
    secondary = Color(0xFFDDDDDA),
    background = Color(0xFF141414),
    onBackground = Color(0xFFF7F7F5),
    surface = Color(0xFF202020),
    surfaceContainer = Color(0xFF222324),
    surfaceContainerHigh = Color(0xFF303234),
    surfaceContainerLow = Color(0xFF1B1C1D),
    onSurface = Color(0xFFF7F7F5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBABABA),
    outline = Color(0xFFBABABA),
    outlineVariant = Color(0xFF484848),
    error = Color(0xFFFFADA6)
)

@Composable
fun NivorynTheme(
    appearance: String = "System",
    reducedMotion: Boolean = false,
    hindi: Boolean = false,
    content: @Composable () -> Unit
) {
    val family = bundledFontFamily(hindi)
    val typography = NivorynTypography.let { t ->
        t.copy(
            displayLarge = t.displayLarge.copy(fontFamily = family),
            displayMedium = t.displayMedium.copy(fontFamily = if (hindi) family else EditorialFont),
            displaySmall = t.displaySmall.copy(fontFamily = family),
            headlineLarge = t.headlineLarge.copy(fontFamily = if (hindi) family else EditorialFont),
            headlineMedium = t.headlineMedium.copy(fontFamily = if (hindi) family else EditorialFont),
            headlineSmall = t.headlineSmall.copy(fontFamily = family),
            titleLarge = t.titleLarge.copy(fontFamily = if (hindi) family else EditorialFont),
            titleMedium = t.titleMedium.copy(fontFamily = family),
            titleSmall = t.titleSmall.copy(fontFamily = family),
            bodyLarge = t.bodyLarge.copy(fontFamily = family),
            bodyMedium = t.bodyMedium.copy(fontFamily = family),
            bodySmall = t.bodySmall.copy(fontFamily = family),
            labelLarge = t.labelLarge.copy(fontFamily = family),
            labelMedium = t.labelMedium.copy(fontFamily = family),
            labelSmall = t.labelSmall.copy(fontFamily = family)
        )
    }
    val dark = appearance == "Dark" || (appearance == "System" && isSystemInDarkTheme())

    val view=androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(dark,view) {
        (view.context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars=!dark
                isAppearanceLightNavigationBars=!dark
            }
        }
    }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(6.dp),
                small = RoundedCornerShape(6.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(16.dp)
            ),
            content = content
        )
    }
}

object Motion {
    const val Press = 120
    const val Enter = 220
    const val Exit = 150
    const val Disclosure = 200
    const val FanOut = 800
    const val CrossFade = 280
}

// Easing curves matching the web
val EaseOutCustom: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EaseInOutCustom: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
val EaseDrawerCustom: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val EaseCustom: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
