package dev.kyzenn.nivoryn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.R
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun WelcomeScreen(hi:Boolean,onStart:()->Unit,onSample:()->Unit) {
    Spacer(Modifier.height(32.dp))
    BrandMark(Modifier.size(44.dp))
    Text("nivoryn",style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(40.dp))
    PageTitle(if(hi)"भरोसा करने से पहले रुकें।"else"Pause before\nyou trust.",if(hi)"किसी संदेश और आपके अगले कदम के बीच थोड़ा संदर्भ।"else"A little context between a message and your next move.")
    Spacer(Modifier.height(16.dp))
    Text(if(hi)"संदिग्ध लिंक, संदेश और फ़ाइल को करीब से समझें।"else"Take a closer look at suspicious links, messages and files.")
    Notice(if(hi)"ऑफ़लाइन पाठ और लिंक की जाँच। स्पष्ट प्रमाण, सीमाएँ और कोई अपलोड नहीं।"else"Offline text and link checks. Clear evidence, explicit limits and no uploads.")
    ActionButton(if(hi)"शुरू करें"else"Start checking",onStart,Modifier.fillMaxWidth())
    ActionButton(if(hi)"उदाहरण आज़माएँ"else"Try a sample",onSample,Modifier.fillMaxWidth(),secondary=true)
    Text(if(hi)"कोई खाता या अनिवार्य अनुमति नहीं।"else"No account. No required permissions.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable fun HomeScreen(draft:CheckDraft,hi:Boolean,onText:(String)->Unit,onReview:()->Unit,onImport:(String)->Unit,onSamples:()->Unit,onClear:()->Unit,onKind:(String)->Unit,isCheck:Boolean=false,onPage:(String)->Unit={}) {
    val clipboard=androidx.compose.ui.platform.LocalClipboard.current
    val scope=rememberCoroutineScope()
    var pasteNotice by remember { mutableStateOf("") }
    fun paste() { scope.launch {
        val text=clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString().orEmpty()
        if(text.isBlank()) pasteNotice=if(hi) "क्लिपबोर्ड में पाठ नहीं है।" else "There is no text on the clipboard."
        else { onText(text.take(12000)); onKind("Text"); pasteNotice="" }
    } }
    PageTitle(if(hi) "क्या जाँचना है?" else if(isCheck) "Check content" else "Check before you act.",
        if(hi) "सामग्री चुनें। प्रमाण देखें।" else if(isCheck) "Choose a mode, then review your content." else "Messages, links and files. Checked on your device.")
    androidx.compose.animation.AnimatedVisibility(!isCheck,
        enter=androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(if(dev.kyzenn.nivoryn.ui.theme.LocalReducedMotion.current) 0 else 220))+androidx.compose.animation.fadeIn(),exit=androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(if(dev.kyzenn.nivoryn.ui.theme.LocalReducedMotion.current) 0 else 220))+androidx.compose.animation.fadeOut()) {
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ActionButton(if(hi) "स्कैन करें" else "Scan", { onPage("scan") }, Modifier.weight(1f))
                ActionButton(if(hi) "पेस्ट करें" else "Paste", ::paste, Modifier.weight(1f), secondary=true)
            }
            Surface(onClick={onPage("model")},color=MaterialTheme.colorScheme.surfaceContainerHigh,shape=MaterialTheme.shapes.small) {
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(if(hi) "बुनियादी जाँच तैयार" else "Basic checks ready",style=MaterialTheme.typography.labelLarge)
                    val ai by (androidx.compose.ui.platform.LocalContext.current.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).modelSetup.state.collectAsState()
                    Text(if(hi) "स्थानीय AI: ${ai.phase}" else "Local AI: ${ai.phase}",style=MaterialTheme.typography.bodySmall)
                    Text(if(ai.ready) "Offline explanations ready ›" else "Set up local AI ›",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if(pasteNotice.isNotBlank()) Notice(pasteNotice)
    LocalDashboard(hi,draft.kind,onKind,onImport,compact=isCheck)
    if(isCheck) GroupedSurface(if(hi) "आपकी सामग्री" else when(draft.kind) { "Link"->"Inspect a link";"Document"->"Choose a document";"Screenshot","QR"->"Scan or import";else->"Review a message" }) {
        if(draft.kind in listOf("Screenshot","QR","Document")) {
            Text(if(draft.kind=="Document") "PDF or image · first-page OCR · up to 20 MB" else "Camera or image import · OCR and QR · up to 20 MB",style=MaterialTheme.typography.bodyMedium)
            ActionButton(if(draft.kind=="Document") "Choose document" else "Scan image",{onImport(draft.kind)},Modifier.fillMaxWidth())
            if(draft.attachmentUri.isNotEmpty()) Text(draft.filename)
        } else {
            FormField(draft.text,if(hi) "संदेश या लिंक" else "Message or link",onText,multiline=true)
            TextAction(if(hi) "क्लिपबोर्ड से पेस्ट करें" else "Paste from clipboard",::paste)
            ActionButton(if(hi) "समीक्षा करें" else if(draft.kind=="Link") "Check link" else "Review message",onReview,Modifier.fillMaxWidth())
        }
        Text(if(hi) "विश्लेषण इसी फ़ोन पर होता है।" else "Analysis stays on this device. Review extracted text before checking.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(draft.canReview) TextAction(if(hi) "ड्राफ़्ट हटाएँ" else "Clear draft",onClear)
    }
    if(isCheck) {
        TextAction(if(hi) "QR छवि चुनें" else "Inspect a QR image",{onImport("QR")})
        TextAction(if(hi) "उदाहरण देखें · कोई वास्तविक जाँच नहीं" else "Explore sample checks · example data",onSamples)
    }
}
@Composable fun SampleList(samples:List<DemoSample>,hi:Boolean,onSelect:(DemoSample)->Unit) {
    PageTitle(if(hi)"उदाहरण चुनें"else"Start with an example",if(hi)"ये लिखे हुए उदाहरण हैं, निजी सामग्री की जाँच नहीं।"else"Authored examples, never verdicts about your own content.")
    samples.forEach { sample ->
        Column { TextAction(if(hi)sample.titleHi else sample.title,{onSelect(sample)})
            Text(sample.kind,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant) }
    }
}
