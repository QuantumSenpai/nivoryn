package dev.kyzenn.nivoryn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kyzenn.nivoryn.ui.theme.*
import dev.kyzenn.nivoryn.ui.components.ProductGlyph
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.domain.DemoResult
import dev.kyzenn.nivoryn.domain.Assessment

/**
 * Phone frame matching the web's Blender-rendered phone:
 * - Orthographic: 640×1200, 160px/world unit
 * - Screen mesh: 2.96×6.03, centre (0, -0.02)
 * - Inner corners: (83.2, 120.8) to (556.8, 1085.6)
 * - Screen: left 13%, top 10.07%, width 74%, height 80.4%
 */
@Composable
fun PhoneFrame(
    modifier: Modifier = Modifier,
    screenContent: @Composable () -> Unit = {},
    showFrame: Boolean = true,
    frameColor: Color = Color.Black,
    screenBackground: Color = Color.White
) {
    val aspectRatio = 1200f / 640f
    val frameWidth = 280.dp
    val frameHeight = (frameWidth.value * aspectRatio).dp

    // Calculate screen padding in Dp based on frame width
    val screenPaddingStart = (frameWidth.value * 0.13f).dp
    val screenPaddingTop = (frameWidth.value * aspectRatio * 0.1007f).dp
    val screenPaddingEnd = (frameWidth.value * 0.13f).dp
    val screenPaddingBottom = (frameWidth.value * aspectRatio * 0.0953f).dp

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .width(frameWidth)
    ) {
        if (showFrame) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val w = size.width
                val h = size.height
                val corner = 32f
                drawRoundRect(
                    frameColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = screenPaddingStart,
                    top = screenPaddingTop,
                    end = screenPaddingEnd,
                    bottom = screenPaddingBottom
                )
                .background(screenBackground, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        ) {
            screenContent()
        }
    }
}

/**
 * Phone screen content for the check/result flow
 */
@Composable
fun PhoneScreenContent(
    draft: CheckDraft? = null,
    result: DemoResult? = null,
    isProcessing: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = BrandRed
            )
            Spacer(Modifier.height(16.dp))
            Text("Analyzing…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (result != null) {
            ResultPhoneView(result)
        } else if (draft != null) {
            DraftPhoneView(draft)
        } else {
            DefaultPhoneView()
        }
    }
}

@Composable
private fun DefaultPhoneView() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("nivoryn", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Pause before\nyou trust.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DraftPhoneView(draft: CheckDraft) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("9:41", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ProductGlyph(5, Modifier.size(12.dp))
                ProductGlyph(6, Modifier.size(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("nivoryn", style = MaterialTheme.typography.titleLarge, fontFamily = EditorialFont, color = MaterialTheme.colorScheme.onSurface)
            Text("Demo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp)
        ) {
            Text(
                draft.text.take(200) + if (draft.text.length > 200) "…" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (draft.attachmentUri.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(draft.filename, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${draft.bytes / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResultPhoneView(result: DemoResult) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val (color, label) = when (result.assessment) {
            Assessment.HIGH -> MaterialTheme.colorScheme.error to "High Risk"
            Assessment.SUSPICIOUS -> Warning to "Suspicious"
            Assessment.NONE -> Positive to "No clear warning"
            else -> MaterialTheme.colorScheme.onSurface to "Unknown"
        }

        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            result.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (result.evidence.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Evidence: ${result.evidence.size} findings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))

        if (result.nextSteps.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    "💡 ${result.nextSteps.first()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Phone frame for hero/splash - larger, with fan-out animation support
 */
@Composable
fun HeroPhoneFrame(
    modifier: Modifier = Modifier,
    fanOutProgress: Float = 0f,
    onPhoneClick: (() -> Unit)? = null
) {
    val aspectRatio = 1200f / 640f
    val frameWidth = 320.dp
    val frameHeight = (frameWidth.value * aspectRatio).dp

    val screenPaddingStart = (frameWidth.value * 0.13f).dp
    val screenPaddingTop = (frameWidth.value * aspectRatio * 0.1007f).dp
    val screenPaddingEnd = (frameWidth.value * 0.13f).dp
    val screenPaddingBottom = (frameWidth.value * aspectRatio * 0.0953f).dp

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .width(frameWidth)
            .fillMaxWidth()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val w = size.width
            val h = size.height
            val corner = 36f
            drawRoundRect(
                Color.Black,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = screenPaddingStart,
                    top = screenPaddingTop,
                    end = screenPaddingEnd,
                    bottom = screenPaddingBottom
                )
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
            ) {
                DefaultPhoneView()
            }

            if (fanOutProgress > 0f) {
                FanOutObjects(fanOutProgress)
            }
        }
    }
}

@Composable
private fun FanOutObjects(progress: Float) {
    val positions = remember {
        listOf(
            Pair(-20.25f, 11.25f),
            Pair(-6f, -27f),
            Pair(8f, -5f),
            Pair(6f, 5f)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        positions.forEachIndexed { index, (dx, dy) ->
            val x = dx * progress * 0.8f
            val y = dy * progress * 0.8f
            val scale = 0.3f + 0.7f * progress

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                ProductGlyph(index, Modifier.size(40.dp))
            }
        }
    }
}
