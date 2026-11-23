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

@Composable fun ExtractionControl(draft: CheckDraft, hi: Boolean, onText: (String) -> Unit, onExtracted: (dev.kyzenn.nivoryn.integration.ExtractionResult) -> Unit = { onText(it.text) }) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember(draft.attachmentUri) { mutableStateOf(false) }
    var message by remember(draft.attachmentUri) { mutableStateOf("") }
    var extractionJob by remember(draft.attachmentUri) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    DisposableEffect(draft.attachmentUri) { onDispose { extractionJob?.cancel() } }
    if (draft.kind !in listOf("Screenshot", "Document", "QR")) return
    Text(if (draft.mime == "application/pdf") "Local extraction · first page only · PDFs up to 20 pages / 20 MB" else "Local extraction · ${if (hi) "Devanagari" else "Latin"} OCR / QR · 20 MB limit", style = MaterialTheme.typography.bodySmall)
    ActionButton(if (busy) "Reading on device…" else if (draft.kind == "QR") "Decode QR on device" else "Extract text on device", {
        busy = true; message = ""
        extractionJob = scope.launch {
            try {
                val extraction = LocalExtraction.readDetailed(context, draft, hi)
                val text = extraction.text
                if (text.isBlank() && extraction.qr.isNotBlank()) { onExtracted(extraction); message = "Only a QR payload was decoded. No OCR text was extracted. Review the QR below; its issuer and content remain unverified." }
                else if (text.isBlank()) message = "No readable text found. Type the text manually or choose a clearer image."
                else { onExtracted(extraction); message = "Text added below. Correct recognition errors before running a check. No destination was opened." }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) { message = "Extraction deadline reached. Try a clearer or smaller file, or enter text manually." }
              catch (e: CancellationException) { throw e }
              catch (_: Exception) { message = "Could not read this file. Choose an image/PDF under 20 MB (PDF: up to 20 pages), or enter text manually." }
            finally { busy = false }
        }
    }, Modifier.fillMaxWidth(), secondary = true, enabled = !busy)
    if (busy) TextAction("Cancel extraction", { extractionJob?.cancel(); message = "Extraction cancelled. No extracted content was analysed." })
    if (message.isNotEmpty()) Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium)
}
