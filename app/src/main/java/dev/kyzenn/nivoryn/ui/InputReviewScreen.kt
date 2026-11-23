package dev.kyzenn.nivoryn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun ReviewScreen(draft:CheckDraft,hi:Boolean,onText:(String)->Unit,onRemove:()->Unit,onReplace:()->Unit,onContinue:()->Unit,onSamples:()->Unit,onExtracted:(dev.kyzenn.nivoryn.integration.ExtractionResult)->Unit = { onText(it.text) }) {
    PageTitle(if(hi)"इनपुट की समीक्षा"else"Review your input",if(hi)"आगे बढ़ने से पहले सामग्री पढ़ें।"else"Read what you brought before taking the next step.")
    Notice(if(draft.sampleId.isNotEmpty()) {if(hi)"लिखा हुआ उदाहरण · संपादन करने पर यह निजी ड्राफ़्ट बन जाएगा।"else"Authored example · Editing makes this a custom draft."}else{if(hi)"निजी ड्राफ़्ट · स्थानीय पाठ और लिंक की जाँच।"else"Custom draft · Local text and link rules. No upload or live verification."})
    if(draft.attachmentUri.isNotEmpty()) {
        SectionTitle(draft.filename)
        Text("${draft.mime} · ${if(draft.bytes>=0)"${draft.bytes / 1024} KB"else if(hi)"आकार अज्ञात"else"Size unavailable"}",style=MaterialTheme.typography.bodyMedium)
        when {
            draft.mime.startsWith("image/") || draft.mime=="application/pdf" -> FilePreview(draft,hi)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { TextAction(if(hi)"फ़ाइल हटाएँ"else"Remove attachment",onRemove);TextAction(if(hi)"बदलें"else"Replace",onReplace) }
        key(draft.attachmentUri) { ExtractionControl(draft, hi, onText, onExtracted) }
        when(draft.kind) {
            "Screenshot", "QR" -> Notice(if(hi) "निकाले गए पाठ की पुष्टि करें। कोई लिंक या भुगतान अपने आप नहीं खुलेगा।" else "Confirm the extracted text below. Links and payment payloads are never opened automatically.")
            "Document" -> Notice("First-page text can be checked. Booking status, signatures and document authenticity remain unverified.")
            "Audio" -> Notice("Audio playback and transcription are unavailable. Paste a transcript using Text to check a message.")
            "APK" -> Notice("This file will not be executed or installed. Malware scanning is unavailable.")
        }
    }

    if(draft.kind=="Link" && draft.sampleId.isNotEmpty()) {
        SectionTitle(if(hi)"लिंक का उदाहरण"else"Example link preview")
        Text("Display text: Your Bank\nActual host: your-bank.support-check.example")
        Notice(if(hi)"दिखने वाला नाम स्वामित्व साबित नहीं करता। लिंक अपने आप नहीं खुलेगा।"else"Display text does not establish ownership. The link is never opened automatically.")
    }
    if(draft.kind=="QR" && draft.sampleId.isNotEmpty()) {
        SectionTitle(if(hi)"भुगतान अनुरोध का उदाहरण"else"Example payment request")
        Text("Payee: Demo Merchant\nAddress: example-merchant@upi\nAmount: ₹999\nRecipient ownership: unverified")
    }
    if (draft.extractionSource.isNotBlank()) Text("Extracted from: ${draft.extractionSource}. OCR text is editable; errors remain possible.")
    if (draft.qrPayload.isNotBlank()) Text("Original decoded QR (not opened): ${draft.qrPayload}")
    FormField(draft.text,if(hi)"संपादन योग्य पाठ / सुधार"else if(draft.kind=="Screenshot")"Your corrected text · type it manually"else"Editable text",onText,multiline=true)
    if(draft.text.length>=12000) Text(if(hi)"12,000 अक्षरों की सीमा पूरी हुई।"else"The 12,000-character draft limit has been reached.")
    ActionButton(if(hi)"उदाहरण परिणाम / स्थानीय जाँच"else if(draft.sampleId.isNotEmpty())"Show example result"else"Run local check",onContinue,Modifier.fillMaxWidth(),enabled=draft.canReview)
    TextAction(if(hi)"तैयार उदाहरण चुनें"else"Choose a predefined example",onSamples)
}

@Composable private fun FilePreview(draft:CheckDraft,hi:Boolean) {
    val uri=draft.attachmentUri; val mime=draft.mime
    val context=LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(uri) { mutableStateOf("") }
    var zoom by rememberSaveable(uri) { mutableFloatStateOf(1f) }
    DisposableEffect(uri) { onDispose { bitmap?.recycle() } }
    LaunchedEffect(uri,mime) {
        try { bitmap=dev.kyzenn.nivoryn.integration.LocalExtraction.preview(context,draft) }
        catch(_:kotlinx.coroutines.TimeoutCancellationException) { error="Preview deadline reached. Replace the file or remove the attachment." }
        catch(e:kotlinx.coroutines.CancellationException) { throw e }
        catch(_:Exception) { error=if(hi)"पूर्वावलोकन उपलब्ध नहीं। फ़ाइल को बदला या हटाया जा सकता है।"else"Preview unavailable. Limits: 20 MB, 20 PDF pages (first page only), bounded image dimensions. Replace or remove this file." }
    }
    if(error.isNotEmpty()) Notice(error)
    else if(bitmap==null) Text(if(hi)"स्थानीय पूर्वावलोकन खुल रहा है…"else"Opening local preview…")
    else {
        Box(Modifier.fillMaxWidth().height(260.dp).graphicsLayer { clip=true }.pointerInput(uri) { detectTransformGestures { _,_,change,_->zoom=(zoom*change).coerceIn(1f,3f) } }) {
            Image(bitmap!!.asImageBitmap(),if(hi)"चुनी हुई फ़ाइल का पूर्वावलोकन"else"Preview of your selected file",Modifier.fillMaxSize().graphicsLayer { scaleX=zoom;scaleY=zoom })
        }
        Row { TextAction(if(hi)"ज़ूम करें"else"Zoom in",{zoom=(zoom+.5f).coerceAtMost(3f)});TextAction(if(hi)"ज़ूम रीसेट"else"Reset zoom",{zoom=1f}) }
        if(mime=="application/pdf") Text(if(hi)"केवल पहला पृष्ठ दिखाया गया है।"else"First page preview only.",style=MaterialTheme.typography.bodySmall)
    }
}
