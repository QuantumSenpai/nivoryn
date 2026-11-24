package dev.kyzenn.nivoryn.ui

import android.app.ActivityManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import dev.kyzenn.nivoryn.NivorynApplication
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun ModelSetupScreen() {
    val context=LocalContext.current
    val setup=remember { (context.applicationContext as NivorynApplication).modelSetup }
    val state by setup.state.collectAsState()
    val progress=remember { BringIntoViewRequester() }
    LaunchedEffect(state.phase) { if(state.phase!="Inspecting") progress.bringIntoView() }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var mobile by rememberSaveable { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("model") }
    var licence by remember { mutableStateOf("") }
    var showLicence by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) setup.import(name,uri) }
    val memory=remember { ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem/1_000_000_000.0 }
    LaunchedEffect(showLicence) { if(showLicence) licence=withContext(Dispatchers.IO) { listOf("LLAMA_NOTICE.txt","LLAMA32_LICENSE.txt").joinToString("\n\n") { context.assets.open(it).bufferedReader().use { reader->reader.readText() } } } }
    PageTitle("Set up local AI","Built with Llama · ExecuTorch · explanations on your device")
    GroupedSurface(state.phase,Modifier.bringIntoViewRequester(progress)) {
        Text(state.detail,Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        if(state.phase in listOf("Downloading","Waiting for network")) {
            LinearProgressIndicator(progress={ (state.downloaded.toFloat()/state.total).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
            Text("${state.downloaded/1_000_000} / ${state.total/1_000_000} MB",style=MaterialTheme.typography.labelLarge)
        }
        if(state.elapsedMs>0) Text("Real inference check: ${state.elapsedMs} ms. This device/sample only.",style=MaterialTheme.typography.bodySmall)
        if(state.busy) TextAction("Cancel setup operation",setup::cancel)
        else if(state.installed) ActionButton("Test local inference",setup::test,Modifier.fillMaxWidth())
    }
    if(!state.ready) GroupedSurface("1 · Prepare your device") {
        Text("${if(Build.SUPPORTED_ABIS.contains("arm64-v8a")) "Arm64 supported" else "Arm64 required"} · Android ${Build.VERSION.RELEASE} · ${"%.1f".format(memory)} GB RAM",style=MaterialTheme.typography.titleSmall)
        Text("Download: 1.14 GB. Allow at least 2.6 GB free for downloading and verified private installation, plus existing resources. 4 GB RAM or more is recommended; readiness depends on the real inference test.",style=MaterialTheme.typography.bodyMedium)
        Text("Basic text, link, OCR and QR checks work without this download. Downloads need connectivity; later analysis runs offline.",style=MaterialTheme.typography.bodySmall)
    }
    if(!state.ready && !state.busy) GroupedSurface("2 · Get the supported model") {
        Text("Llama 3.2 1B Instruct · SpinQuant INT4. The exact published conversion is linked by official ExecuTorch documentation and pinned to a reviewed revision and hashes.",style=MaterialTheme.typography.bodyMedium)
        TextAction("Read Llama licence and notice",{showLicence=true})
        ExternalLink("Publisher and access instructions","https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET")
        SettingSwitch("Licence reviewed","I have reviewed and meet the Llama Community Licence and acceptable use terms.",accepted,{accepted=it})
        SettingSwitch("Allow mobile data","Off by default. This is a large download; roaming stays disabled.",mobile,{mobile=it})
        ActionButton("Download and verify local AI",{setup.download(mobile,accepted)},Modifier.fillMaxWidth(),enabled=accepted)
        Text("Android automatically pauses/retries when conditions require it and resumes after network recovery. Manual pause is not supported by this download service. Cancel removes the downloaded copies; Retry starts a fresh request.",style=MaterialTheme.typography.bodySmall)
    }
    GroupedSurface(if(state.ready) "Offline explanations are ready" else "3 · Verify, prepare and test") {
        Text(if(state.ready) "A real Llama explanation passed validation on this installation. Results still show their actual explanation mode; individual generations can fall back." else "Downloads must pass exact size and SHA-256 checks, private pair activation and a real Llama inference check. Download completion alone never marks setup ready.",style=MaterialTheme.typography.bodyMedium)
        if(state.ready) TextAction("Run another inference check",setup::test)
        Text("If access becomes gated, use the publisher's legitimate licence/account steps and the advanced import below. Never paste account tokens into Nivoryn.",style=MaterialTheme.typography.bodySmall)
    }
    TextAction(if(advanced) "Hide advanced options" else "Advanced: import existing resources",{advanced=!advanced})
    if(advanced && !state.busy) GroupedSurface("Existing compatible resources") {
        Text("Select the exact supported .pte export and matching tokenizer.model/tokenizer.bin. Arbitrary exports, GGUF and original training weights are refused.")
        TextAction("Import model export",{name="model";picker.launch(arrayOf("application/octet-stream","*/*"))})
        TextAction("Import matching official tokenizer",{name="tokenizer";picker.launch(arrayOf("application/octet-stream","*/*"))})
        TextAction("Roll back to previous trusted version",setup::rollback)
    }
    if(!state.busy) TextAction("Delete model resources",{confirmDelete=true})
    if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Delete local models?")},text={Text("Removes active, previous and partial model resources and downloaded copies. Saved checks stay encrypted and unchanged.")},confirmButton={TextAction("Delete models",{confirmDelete=false;setup.delete()})},dismissButton={TextAction("Keep models",{confirmDelete=false})})
    if(showLicence) AlertDialog(onDismissRequest={showLicence=false},title={Text("Built with Llama")},text={Text(licence.ifBlank { "Loading bundled licence…" },Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextAction("Close licence",{showLicence=false})})
}
