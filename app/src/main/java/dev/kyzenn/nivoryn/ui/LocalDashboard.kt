package dev.kyzenn.nivoryn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.ui.components.ProductGlyph
import dev.kyzenn.nivoryn.ui.theme.*

/** Stable touch targets: two columns on compact phones, four when labels fit. */
@Composable fun LocalDashboard(hi: Boolean, kind: String, onKind: (String) -> Unit, onImport: (String) -> Unit) {
    val selected = when(kind) { "Link" -> 0; "Screenshot" -> 2; "Document" -> 3; else -> 1 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (hi) "ऑफ़लाइन · इसी फ़ोन पर" else "OFFLINE · ON THIS DEVICE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
            val columns = if (maxWidth < 340.dp) 2 else 4
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).chunked(columns).forEach { indices ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        indices.forEach { index ->
                            val active = selected == index
                            val line by animateColorAsState(if (active) BrandRed else MaterialTheme.colorScheme.outlineVariant,
                                tween(if (LocalReducedMotion.current) 0 else 120), label = "input selection")
                            Column(Modifier.weight(1f).heightIn(min = 72.dp)
                                .border(1.dp, line, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .selectable(active, role = Role.RadioButton, onClick = {
                                    when(index) { 0 -> onKind("Link"); 1 -> onKind("Text"); 2 -> onImport("Screenshot"); else -> onImport("Document") }
                                }).semantics { stateDescription = if (active) (if (hi) "चयनित" else "Selected") else "" }.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ProductGlyph(index, Modifier.size(22.dp))
                                Text(if (hi) listOf("लिंक", "संदेश", "छवि", "फ़ाइल")[index] else listOf("Link", "Message", "Screenshot", "Document")[index], style = MaterialTheme.typography.labelSmall)
                                Canvas(Modifier.size(4.dp)) { if (active) drawCircle(BrandRed) }
                            }
                        }
                    }
                }
            }
        }
    }
}
