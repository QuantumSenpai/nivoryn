package dev.kyzenn.nivoryn.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.ui.theme.*

@Composable fun BrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics { contentDescription = "Nivoryn" }) {
        val s = size.minDimension / 64f
        drawRoundRect(BrandRed, cornerRadius = CornerRadius(14*s))
        val path = Path().apply {
            moveTo(21*s,46*s); lineTo(21*s,20*s)
            moveTo(21*s,29*s); cubicTo(34*s,12*s,47*s,20*s,43*s,36*s); lineTo(43*s,46*s)
        }
        drawPath(path, Color(0xFF191919), style = Stroke(5.5f*s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Short launch signature, not a loader. No model or file preparation gates entry. */
@Composable fun SplashScreen(onComplete: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(0f) }
    val latestComplete by rememberUpdatedState(onComplete)
    LaunchedEffect(Unit) {
        if (reduced) { latestComplete(); return@LaunchedEffect }
        progress.animateTo(.75f, tween(660, easing = androidx.compose.animation.core.LinearEasing))
        latestComplete()
        progress.animateTo(1f, tween(180, easing = androidx.compose.animation.core.LinearEasing))
    }
    fun phase(start: Float, end: Float): Float = EaseOutCustom.transform(((progress.value-start)/(end-start)).coerceIn(0f,1f))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.graphicsLayer {
            val exit = phase(.75f,1f)
            alpha = 1f-exit
            translationY = -14.dp.toPx()*exit
        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            BrandMark(Modifier.size(76.dp).graphicsLayer {
                val entrance = phase(0f,.30f)
                alpha = entrance
                scaleX = .90f+.10f*entrance; scaleY = scaleX
                translationY = 12.dp.toPx()*(1f-entrance)
                rotationZ = -6f*(1f-entrance)
            })
            Text("nivoryn", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.graphicsLayer {
                val entrance = phase(.12f,.42f)
                alpha = entrance; translationY = 10.dp.toPx()*(1f-entrance)
            })
            Text("Pause before you trust.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.graphicsLayer {
                alpha = phase(.24f,.50f)
            })
        }
    }
}
