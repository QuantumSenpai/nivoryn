package dev.kyzenn.nivoryn.ui
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun SettingsScreen(hi:Boolean,appearance:String,reduced:Boolean,onAppearance:(String)->Unit,onReduced:(Boolean)->Unit,onPage:(String)->Unit,onClear:()->Unit,onIntro:()->Unit) {
    PageTitle(if(hi)"अपना Nivoryn"else"Make yourself at home",if(hi)"इस डेमो की भाषा, रूप और स्थानीय डेटा।"else"Appearance, comfort and the data you choose to keep.")
    SectionTitle(if(hi)"भाषा"else"Language")
    Text(if(hi)"ऊपर English बटन से भाषा बदलें। अंग्रेज़ी उदाहरण के मूल साक्ष्य उसी भाषा में रहते हैं।"else"Use हिन्दी in the app bar to switch language. Original example evidence stays in its source language.")
    SectionTitle(if(hi)"रूप"else"Appearance")
    listOf("System","Light","Dark").forEachIndexed { index,value ->
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(appearance==value,{onAppearance(value)},modifier=Modifier.semantics {contentDescription=if(hi)listOf("सिस्टम","हल्का","गहरा")[index]else value})
            Text(if(hi)listOf("सिस्टम के अनुसार","हल्का","गहरा")[index]else value)
        }
    }
    SettingSwitch(if(hi)"कम गति"else"Reduced motion",if(hi)"सजावटी और पृष्ठ की गति बंद। सिस्टम की सेटिंग का भी सम्मान होता है।"else"Remove spatial movement. The system’s disabled-animation setting is also respected.",reduced,onReduced)
    SectionTitle(if(hi)"डेमो डेटा"else"Your saved data")
    TextAction(if(hi)"सहेजे केस हटाएँ"else"Clear saved checks and cases",onClear)
    TextAction(if(hi)"परिचय फिर देखें"else"Revisit the introduction",onIntro)
    SectionTitle(if(hi)"ऐप के बारे में"else"About this app")
    listOf("model" to if(hi)"मॉडल की उपलब्धता"else"Model and checking availability","privacy" to if(hi)"गोपनीयता"else"Privacy","accessibility" to if(hi)"सुलभता"else"Accessibility","notices" to if(hi)"ओपन-सोर्स और नोटिस"else"About and open-source notices").forEach { (route,label)->TextAction(label,{onPage(route)}) }
    Text("Nivoryn · 0.1.0 · Local rules v1",style=MaterialTheme.typography.bodySmall)
}
@Composable fun InformationScreen(page:String,hi:Boolean) {
    val content=when(page) {
        "privacy" -> listOf("Privacy draft" to "Implementation-specific · owner review required",
            "Your input" to "Local rules inspect confirmed text and URL structure. Bundled OCR reads selected images / the first PDF page; QR decoding reads a payload without opening it. No AI reasoning model runs. No input is uploaded, logged or added to a URL.",
            "Temporary drafts" to "Editable text, selected-file metadata and navigation are retained in Android saved instance state for recreation. The operating system can retain this state with the task. Clear the draft or remove the app task to discard it; this is not a secure-erasure guarantee.",
            "Saved locally" to "Language, appearance, reduced motion, introduction status and demo cases are saved in app-private SharedPreferences. Demo cases contain example IDs and names. Explicitly saved custom checks contain text, input kind and save time, encrypted with AES-GCM and an Android Keystore key in no-backup storage. Up to 50 checks are retained for 30 days; expiration runs when history is read. Delete a check in History or clear all saved data in Settings. Files and attachment URIs are not saved to history.",
            "Files and permissions" to "The Android document picker grants access only to files you select. The app may retain read permission to restore previews; removing an attachment releases that permission. Images are downsampled; PDF preview and OCR use the first page. Extraction caps file reads at 20 MB. PDF extraction creates a temporary app-cache copy, removed after processing; a process crash can leave a cache copy until Android clears cache. No secure-erasure guarantee is made. Audio plays only after you press Play, and pauses when the app backgrounds.",
            "Network and export" to "The app declares no Internet permission and has no remote service, accounts or export feature. Processing stays in the app; imported libraries cannot make network calls through this application UID. Official reporting and source-code links open your browser only when selected and need connectivity.",
            "Backups and owner details" to "Android backup is disabled. Device-transfer behaviour can vary by manufacturer; app-private storage is not a guarantee against a compromised device. Organisation name and privacy contact are for the owner to complete before release. This draft is not a legal review.")
        "accessibility" -> listOf("Accessibility" to "A calm interface should be usable by more people.",
            "Reading and controls" to "Text follows system scaling. Primary controls use a minimum 48dp target, readable labels and visible states. Evidence is presented as text, with labelled disclosures. Decorative scenery is excluded from TalkBack.",
            "Motion and appearance" to "Choose System, Light or Dark in Settings. Reduced motion removes page displacement and disclosure motion. There is no autoplay audio, flashing content or moving text.",
            "Language and feedback" to "English and Hindi navigation are available. Evidence quotes remain in their source language. Noto fonts are bundled locally. Screen reader, keyboard, switch-access and large-text testing on physical hardware remains necessary before a production release.")
        "model" -> listOf("Checking availability" to "Limited, deterministic offline checking is available. Its scope is explicit; no detection accuracy is claimed.",
            "Available now" to "Local message rules; URL and UPI payload structure; Latin / Devanagari OCR selected by app language; QR decoding; editable drafts; first-page PDF and image previews; audio playback; encrypted saved text checks; demo timelines and guidance.",
            "Future integration" to "A scam classifier, Llama / ExecuTorch reasoning, document signature / booking verification, audio transcription and malware scanning are not connected. Bundled OCR models are not scam classifiers. Do not treat unavailable checks as passed.")
        else -> listOf("About Nivoryn" to "Check before you trust. · Frontend demo 0.1.0",
            "Project" to "A native Kotlin and Jetpack Compose frontend for an offline-first scam-awareness application for India. Local rule matches are heuristic evidence, not a complete scam detector. No detection accuracy or production readiness is claimed.",
            "Open source" to "Application code follows the repository LICENSE. AndroidX and Kotlin dependencies carry their respective Apache 2.0 and other notices. Noto Sans, Noto Serif and Noto Sans Devanagari are bundled under the SIL Open Font License 1.1.",
            "Artwork" to "Original graphite product objects authored in Blender. The product-led design uses Manrope and DM Sans under SIL OFL 1.1, with Noto Sans Devanagari for Hindi. Previous scenery is archived and is not part of the active design.",
            "Model weights" to "No model weights or runtime are included. Future model assets require their own licence review.")
    }
    PageTitle(if(hi)when(page){"privacy"->"गोपनीयता का प्रारूप";"accessibility"->"सुलभता";"model"->"जाँच की उपलब्धता";else->"Nivoryn के बारे में"}else content.first().first)
    if(hi) Notice("इस संस्करण में विस्तृत तकनीकी नोटिस अंग्रेज़ी में हैं। निजी सामग्री अपलोड नहीं होती। स्थानीय नियम, OCR और QR की सीमित जाँच उपलब्ध है।")
    Text(content.first().second)
    content.drop(1).forEach { (title,body)->SectionTitle(title);Text(body) }
    if(page=="notices") {
        val context=LocalContext.current
        var full by remember { mutableStateOf(false) }
        TextAction(if(full)"Hide font licence"else"Read bundled font licence",{full=!full})
        if(full) Text(remember{context.assets.open("FONT_LICENSE.txt").bufferedReader().use{it.readText()}},style=MaterialTheme.typography.bodyMedium)
        ExternalLink("View source repository","https://github.com/QuantumSenpai/nivoryn")
    }
}
@Composable fun ExternalLink(label:String,url:String) {
    val context=LocalContext.current
    var error by remember {mutableStateOf("")}
    TextAction(label,{ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }.onFailure { error="No browser is available to open this link." } })
    if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
}
