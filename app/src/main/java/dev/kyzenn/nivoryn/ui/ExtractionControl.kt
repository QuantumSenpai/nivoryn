package dev.kyzenn.nivoryn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.integration.LocalExtraction
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable fun ExtractionControl(draft: CheckDraft, hi: Boolean, onText: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember(draft.attachmentUri) { mutableStateOf(false) }
    var message by remember(draft.attachmentUri) { mutableStateOf("") }
    if (draft.kind !in listOf("Screenshot", "Document", "QR")) return
    Text(if (draft.mime == "application/pdf") "Local extraction · first PDF page only · 20 MB limit" else "Local extraction · ${if (hi) "Devanagari" else "Latin"} OCR / QR · 20 MB limit", style = MaterialTheme.typography.bodySmall)
    ActionButton(if (busy) "Reading on device…" else if (draft.kind == "QR") "Decode QR on device" else "Extract text on device", {
        busy = true; message = ""
        scope.launch {
            try {
                val text = LocalExtraction.read(context, draft, hi)
                if (text.isBlank()) message = "No readable text found. Type the text manually or choose a clearer image."
                else { onText(text); message = "Text added below. Correct recognition errors before running a check. No destination was opened." }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { message = "Could not read this file. Choose an image/PDF under 20 MB, or enter the text manually." }
            finally { busy = false }
        }
    }, Modifier.fillMaxWidth(), secondary = true, enabled = !busy)
    if (message.isNotEmpty()) Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
}
