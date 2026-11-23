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
@Composable fun HomeScreen(draft:CheckDraft,hi:Boolean,onText:(String)->Unit,onReview:()->Unit,onImport:(String)->Unit,onSamples:()->Unit,onClear:()->Unit,onKind:(String)->Unit,isCheck:Boolean=false) {
    Spacer(Modifier.height(12.dp))
    PageTitle(if(isCheck) { if(hi)"जाँच के लिए सामग्री"else"Your input, in context." } else { if(hi)"आप क्या जाँचना चाहते हैं?"else"What do you\nwant to check?" },if(hi)"जल्दबाज़ी की जगह थोड़ा संदर्भ।"else"An unexpected request can wait.")
    LocalDashboard(hi, draft.kind, onKind, onImport)
    HomeForm(draft,hi,onText,onReview,onImport,onSamples,onClear)
}
@Composable private fun HomeForm(draft:CheckDraft,hi:Boolean,onText:(String)->Unit,onReview:()->Unit,onImport:(String)->Unit,onSamples:()->Unit,onClear:()->Unit) {
    val clipboard=androidx.compose.ui.platform.LocalClipboard.current
    val pasteScope = rememberCoroutineScope()
    var pasteNotice by remember { mutableStateOf("") }
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        FormField(draft.text,if(hi)"संदेश या लिंक"else"Message or link",onText,multiline=true)
        TextAction(if(hi)"क्लिपबोर्ड से पेस्ट करें"else"Paste from clipboard",{ pasteScope.launch { val text=clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString(); if(text.isNullOrEmpty()) pasteNotice=if(hi)"क्लिपबोर्ड में पाठ नहीं है।"else"There is no text on the clipboard." else { onText(text.take(12000));pasteNotice="" } } })
        if(pasteNotice.isNotEmpty()) Text(pasteNotice,style=MaterialTheme.typography.bodyMedium)
        if(draft.attachmentUri.isNotEmpty()) Notice("${draft.filename}\n${if(hi)"समीक्षा के लिए चुनी फ़ाइल"else"Selected for review"}")
        ActionButton(if(hi)"इनपुट की समीक्षा करें"else"Review input",onReview,Modifier.fillMaxWidth())
        Text(if(hi)"पाठ और लिंक की संरचना इसी फ़ोन पर जाँची जाती है। ड्राफ़्ट केवल इस सत्र में रहता है।"else"Text and URL structure are checked locally. Your draft stays in this app session.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(draft.canReview) TextAction(if(hi)"ड्राफ़्ट हटाएँ"else"Clear draft",onClear)
        var moreImports by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        TextAction(if (hi) "अन्य आयात विकल्प" else "More import options", { moreImports = !moreImports })
        androidx.compose.animation.AnimatedVisibility(moreImports,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(if (dev.kyzenn.nivoryn.ui.theme.LocalReducedMotion.current) 0 else 160)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(if (dev.kyzenn.nivoryn.ui.theme.LocalReducedMotion.current) 0 else 120))) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("QR", "Audio", "APK").forEach { kind -> TextAction(kind, { onImport(kind) }) }
            }
        }
        TextAction(if(hi)"उदाहरण देखें · कोई वास्तविक जाँच नहीं"else"Explore sample checks · example data",onSamples)
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
