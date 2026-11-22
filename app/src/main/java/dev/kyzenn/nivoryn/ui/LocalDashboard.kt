package dev.kyzenn.nivoryn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.ui.components.ProductGlyph
import dev.kyzenn.nivoryn.ui.theme.*

/** A quiet instrument panel: state and entry points, without pretend live telemetry. */
@Composable fun LocalDashboard(hi: Boolean, kind: String, onKind: (String) -> Unit, onImport: (String) -> Unit) {
    val selected = when(kind) { "Link" -> 0; "Screenshot" -> 2; "Document" -> 3; else -> 1 }
    val red = BrandRed
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (hi) "ऑफ़लाइन · इसी फ़ोन पर" else "OFFLINE · ON THIS DEVICE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Link", "Message", "Screenshot", "Document").forEachIndexed { index, kind ->
                    Column(Modifier.weight(1f).heightIn(min = 64.dp)
                        .border(1.dp, if (selected == index) red else line, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { if (index >= 2) onImport(kind) else onKind(if (index == 0) "Link" else "Text") }
                        .semantics { stateDescription = if (selected == index) "Selected" else "" }
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProductGlyph(index, Modifier.size(22.dp))
                        Text(if (hi) listOf("लिंक", "संदेश", "छवि", "फ़ाइल")[index] else kind, style = MaterialTheme.typography.labelSmall)
                        if (selected == index) Canvas(Modifier.size(4.dp)) { drawCircle(red) }
                        else Spacer(Modifier.height(4.dp))
                    }
                }
            }
    }
}
