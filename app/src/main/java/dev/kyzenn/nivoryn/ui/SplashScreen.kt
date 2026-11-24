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
