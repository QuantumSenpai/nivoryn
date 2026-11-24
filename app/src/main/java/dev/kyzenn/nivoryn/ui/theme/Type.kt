package dev.kyzenn.nivoryn.ui.theme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import dev.kyzenn.nivoryn.R
val UiFont = FontFamily(Font(R.font.dm_sans))
val EditorialFont = FontFamily(Font(R.font.manrope, FontWeight.Medium), Font(R.font.manrope, FontWeight.SemiBold))
private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, display: Boolean = false) = TextStyle(fontFamily = if(display) EditorialFont else UiFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing=if(display) (-.8).sp else 0.sp)
val NivorynTypography = Typography(
    displayMedium = style(36, 42, FontWeight.Medium, true), headlineLarge = style(32, 38, FontWeight.Medium, true), headlineMedium = style(28, 34, FontWeight.Medium, true),
    titleLarge = style(24, 30, FontWeight.Medium, true), titleMedium = style(18, 25, FontWeight.Medium),
    bodyLarge = style(16, 24), bodyMedium = style(14, 22), bodySmall = style(13, 20),
    labelLarge = style(14, 20, FontWeight.Medium), labelMedium = style(13, 20)
)
