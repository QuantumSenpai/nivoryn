package dev.kyzenn.nivoryn.ui
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun SettingsScreen(hi:Boolean,appearance:String,reduced:Boolean,onAppearance:(String)->Unit,onReduced:(Boolean)->Unit,onPage:(String)->Unit,onClear:()->Unit,onIntro:()->Unit) {
    PageTitle(if(hi)"अपना Nivoryn"else"Make yourself at home",if(hi)"इस डेमो की भाषा, रूप और स्थानीय डेटा।"else"Appearance, comfort and the data you choose to keep.")
    SectionTitle("On-device engines")
    TextAction("Rules and indicators", { onPage("rules") })
    TextAction("Llama resources", { onPage("model") })
    TextAction("Network replay", { onPage("network") })
    TextAction("APK indicators", { onPage("apk") })
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
    Text("Nivoryn · ${dev.kyzenn.nivoryn.BuildConfig.VERSION_NAME} · Local rules v2",style=MaterialTheme.typography.bodySmall)
}
@Composable fun InformationScreen(page:String,hi:Boolean) {
    val content=when(page) {
        "privacy" -> listOf("Privacy draft" to "Implementation-specific · owner review required",
            "Your input" to "Local rules inspect confirmed text and URL structure. Bundled OCR reads selected images / the first PDF page; QR decoding reads a payload without opening it. A bundled historical-English SMS classifier can add a spam-like signal. Llama reasoning runs locally only after matching trusted resources are installed; the result names the actual explanation mode. Raw input is excluded from model prompts. No input is uploaded or added to a URL.",
            "Temporary drafts" to "Editable text, selected-file metadata and navigation are retained in Android saved instance state for recreation. The operating system can retain this state with the task. Clear the draft or remove the app task to discard it; this is not a secure-erasure guarantee.",
            "Saved locally" to "Language, appearance, reduced motion, introduction status and demo cases use app-private preferences. Explicitly saved checks include reviewed text, decoded QR/source metadata and the exact evidence/assessment/explanation-mode snapshot, encrypted with AES-GCM and an Android Keystore key in no-backup storage. Up to 50 checks are retained for 30 days with a total storage limit; expiration runs when history is read. Delete checks in History or clear saved data in Settings. Original files and attachment URIs are not copied to history.",
            "Files and permissions" to "The document picker grants access to selected files. Removing an attachment releases its retained read permission. Preview and extraction share a 20 MB byte limit and bounded image dimensions. PDFs may contain up to 20 pages; only page 1 is rendered. A temporary private cache copy is removed after processing; a crash can leave it until cache cleanup. Cancellation/deadlines cannot forcibly interrupt every blocking provider/native call. Audio playback/transcription are unavailable; paste a transcript as Text instead.",
            "Network and export" to "The app declares no Internet permission and has no remote inference service or account. Deliberate JSON export includes original text and evidence; the destination provider controls that plaintext copy. Processing stays in the app; imported libraries cannot make network calls through this application UID. Official reporting and source-code links open your browser only when selected and need connectivity.",
            "Backups and owner details" to "Android backup is disabled. Device-transfer behaviour can vary by manufacturer; app-private storage is not a guarantee against a compromised device. Organisation name and privacy contact are for the owner to complete before release. This draft is not a legal review.")
        "accessibility" -> listOf("Accessibility" to "A calm interface should be usable by more people.",
            "Reading and controls" to "Text follows system scaling. Primary controls use a minimum 48dp target, readable labels and visible states. Evidence is presented as text, with labelled disclosures. Decorative scenery is excluded from TalkBack.",
            "Motion and appearance" to "Choose System, Light or Dark in Settings. Reduced motion removes page displacement and disclosure motion. There is no autoplay audio, flashing content or moving text.",
            "Language and feedback" to "English and Hindi navigation are available. Evidence quotes remain in their source language. Noto fonts are bundled locally. Screen reader, keyboard, switch-access and large-text testing on physical hardware remains necessary before a production release.")
        "model" -> listOf("Checking availability" to "Local detection and bounded advisory generation have explicit limits. Read each result's actual explanation mode and evidence.",
            "Available now" to "Message rules in English/Hindi/Hinglish; a historical-English spam proxy; offline URL/UPI checks; bundled Latin/Devanagari OCR; QR and first-page PDF extraction; local PCAP flow replay; static APK indicators; encrypted history. Llama/ExecuTorch generation runs after the pinned model and tokenizer are installed.",
            "Supported limits" to "Live Android VPN capture, TLS plaintext inspection, general malware behaviour monitoring, issuer/booking/signature authentication and audio processing are unavailable. The spam proxy is not scam ground truth; heuristics and generated prose do not prove fraud or safety. Rejected inference uses a labelled curated fallback.")
        else -> listOf("About Nivoryn" to "Check before you trust. · On-device development build",
            "Project" to "A native Kotlin and Jetpack Compose application for local security checks. Local rule matches are heuristic evidence, not a complete scam detector. No detection accuracy or production readiness is claimed.",
            "Open source" to "Application code follows the repository LICENSE. AndroidX and Kotlin dependencies carry their respective Apache 2.0 and other notices. Noto Sans, Noto Serif and Noto Sans Devanagari are bundled under the SIL Open Font License 1.1.",
            "Artwork" to "Original graphite product objects authored in Blender. The product-led design uses Manrope and DM Sans under SIL OFL 1.1, with Noto Sans Devanagari for Hindi. Previous scenery is archived and is not part of the active design.",
            "Model weights" to "Built with Llama. ExecuTorch 1.2.0 and a small statistical SMS classifier are included. The pinned Llama 3.2 1B SpinQuant export/tokenizer are installed separately; read their offline licence/notice under Llama resources. The SMS proxy uses attributed UCI data under CC BY 4.0 and does not replace Llama. ML Kit SDK/bundled resources follow Google's SDK terms; ZXing/AndroidX follow their respective notices. See the source licence inventory for versions and boundaries.")
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
    TextAction(label,{ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,url.toUri())) }.onFailure { error="No browser is available to open this link." } })
    if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
}
