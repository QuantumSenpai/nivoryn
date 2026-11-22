package dev.kyzenn.nivoryn.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable fun ReviewScreen(draft:CheckDraft,hi:Boolean,onText:(String)->Unit,onRemove:()->Unit,onReplace:()->Unit,onContinue:()->Unit,onSamples:()->Unit) {
    PageTitle(if(hi)"इनपुट की समीक्षा"else"Review your input",if(hi)"आगे बढ़ने से पहले सामग्री पढ़ें।"else"Read what you brought before taking the next step.")
    Notice(if(draft.sampleId.isNotEmpty()) {if(hi)"लिखा हुआ उदाहरण · संपादन करने पर यह निजी ड्राफ़्ट बन जाएगा।"else"Authored example · Editing makes this a custom draft."}else{if(hi)"निजी ड्राफ़्ट · स्थानीय पाठ और लिंक की जाँच।"else"Custom draft · Local text and link rules. No upload or live verification."})
    if(draft.attachmentUri.isNotEmpty()) {
        SectionTitle(draft.filename)
        Text("${draft.mime} · ${if(draft.bytes>=0)"${draft.bytes / 1024} KB"else if(hi)"आकार अज्ञात"else"Size unavailable"}",style=MaterialTheme.typography.bodyMedium)
        when {
            draft.kind=="Audio" -> AudioPreview(draft.attachmentUri,hi)
            draft.mime.startsWith("image/") || draft.mime=="application/pdf" -> FilePreview(draft.attachmentUri,draft.mime,hi)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { TextAction(if(hi)"फ़ाइल हटाएँ"else"Remove attachment",onRemove);TextAction(if(hi)"बदलें"else"Replace",onReplace) }
        key(draft.attachmentUri) { ExtractionControl(draft, hi, onText) }
        when(draft.kind) {
            "Screenshot", "QR" -> Notice(if(hi) "निकाले गए पाठ की पुष्टि करें। कोई लिंक या भुगतान अपने आप नहीं खुलेगा।" else "Confirm the extracted text below. Links and payment payloads are never opened automatically.")
            "Document" -> Notice("First-page text can be checked. Booking status, signatures and document authenticity remain unverified.")
            "Audio" -> Notice("Playback only. Transcription, speaker identity and deepfake detection are unavailable.")
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
    FormField(draft.text,if(hi)"संपादन योग्य पाठ / सुधार"else if(draft.kind=="Screenshot")"Your corrected text · type it manually"else"Editable text",onText,multiline=true)
    if(draft.text.length>=12000) Text(if(hi)"12,000 अक्षरों की सीमा पूरी हुई।"else"The 12,000-character draft limit has been reached.")
    ActionButton(if(hi)"उदाहरण परिणाम / स्थानीय जाँच"else if(draft.sampleId.isNotEmpty())"Show example result"else"Run local check",onContinue,Modifier.fillMaxWidth(),enabled=draft.canReview)
    TextAction(if(hi)"तैयार उदाहरण चुनें"else"Choose a predefined example",onSamples)
}

@Composable private fun FilePreview(uri:String,mime:String,hi:Boolean) {
    val context=LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(uri) { mutableStateOf("") }
    var zoom by rememberSaveable(uri) { mutableFloatStateOf(1f) }
    LaunchedEffect(uri) {
        runCatching {
            withContext(Dispatchers.IO) {
                if(mime=="application/pdf") {
                    context.contentResolver.openFileDescriptor(Uri.parse(uri),"r")!!.use { fd -> PdfRenderer(fd).use { pdf ->
                        require(pdf.pageCount>0); pdf.openPage(0).use { page ->
                            val width=800; val height=(width.toFloat()*page.height/page.width).toInt().coerceIn(1,1600)
                            Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also { it.eraseColor(android.graphics.Color.WHITE);page.render(it,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                        }
                    } }
                } else {
                    val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    context.contentResolver.openInputStream(Uri.parse(uri))!!.use { BitmapFactory.decodeStream(it,null,bounds) }
                    require(bounds.outWidth>0 && bounds.outHeight>0) { "Unsupported or unreadable image" }
                    val options=BitmapFactory.Options().apply { inSampleSize=generateSequence(1){it*2}.first { bounds.outWidth/it<=1200 && bounds.outHeight/it<=1200 } }
                    context.contentResolver.openInputStream(Uri.parse(uri))!!.use { BitmapFactory.decodeStream(it,null,options) ?: error("Unreadable image") }
                }
            }
        }.onSuccess { bitmap=it }.onFailure { error=if(hi)"पूर्वावलोकन उपलब्ध नहीं। फ़ाइल को बदला या हटाया जा सकता है।"else"Preview unavailable. The file may be unsupported, protected or no longer accessible. Replace or remove it." }
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
@Composable private fun AudioPreview(uri:String,hi:Boolean) {
    val context=LocalContext.current;val lifecycle=LocalLifecycleOwner.current.lifecycle
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember(uri) { mutableStateOf(false) };var playing by remember(uri) { mutableStateOf(false) }
    var duration by remember(uri) { mutableIntStateOf(0) };var position by remember(uri) { mutableIntStateOf(0) };var error by remember(uri) { mutableStateOf("") }
    DisposableEffect(uri,lifecycle) {
        val mp=MediaPlayer();player=mp
        runCatching {
            mp.setDataSource(context,Uri.parse(uri))
            mp.setOnPreparedListener { ready=true;duration=it.duration }
            mp.setOnCompletionListener { playing=false;position=duration }
            mp.setOnErrorListener { _,_,_->error="Playback is unavailable for this file.";playing=false;true }
            mp.prepareAsync()
        }.onFailure { error="This audio file could not be opened." }
        val observer=LifecycleEventObserver { _,event->if(event==Lifecycle.Event.ON_STOP && ready) { runCatching{mp.pause()};playing=false } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);mp.release();player=null }
    }
    LaunchedEffect(playing) { while(playing) { position=runCatching { player?.currentPosition ?: 0 }.getOrDefault(0);delay(250) } }
    if(error.isNotEmpty()) Notice(error)
    else {
        fun time(ms:Int)="${ms/60000}:${(ms/1000%60).toString().padStart(2,'0')}"
        Text("${time(position)} / ${time(duration)}",style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum"))
        Slider(position.toFloat(),{position=it.toInt();runCatching{player?.seekTo(position)}},valueRange=0f..duration.coerceAtLeast(1).toFloat(),enabled=ready,modifier=Modifier.semantics { contentDescription=if(hi)"प्लेबैक स्थिति"else"Playback position" })
        ActionButton(if(playing){if(hi)"रोकें"else"Pause"}else{if(hi)"चलाएँ"else"Play audio"},{ runCatching { if(playing)player?.pause() else { if(position>=duration){player?.seekTo(0);position=0};player?.start() };playing=!playing }.onFailure {error="Playback failed. Try another file."} },enabled=ready,secondary=true)
    }
}
