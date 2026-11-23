package dev.kyzenn.nivoryn.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.reasoning.ModelImporter
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun ModelSetupScreen() {
    val context = LocalContext.current
    val store = remember { ModelStore(context) }
    val scope = rememberCoroutineScope()
    val runner = remember { (context.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).llama }
    var importJob by remember { mutableStateOf<Job?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Checking installed resources…") }
    var name by remember { mutableStateOf("model") }
    var busy by remember { mutableStateOf(false) }
    var showLicence by remember { mutableStateOf(false) }
    var licence by remember { mutableStateOf("") }
    LaunchedEffect(showLicence) { if (showLicence) licence = withContext(Dispatchers.IO) {
        listOf("LLAMA_NOTICE.txt", "LLAMA32_LICENSE.txt").joinToString("\n\n") { file -> context.assets.open(file).bufferedReader().use { it.readText() } }
    } }
    LaunchedEffect(Unit) { status = withContext(Dispatchers.IO) {
        if (store.installed() == null) "Llama unavailable: no verified model and tokenizer installed." else "Verified Llama resources installed. Inference has not been tested by this screen."
    } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importJob = scope.launch {
            busy = true
            status = try {
                val job = currentCoroutineContext().job
                runner.manageResources {
                    ModelImporter(store).import(name, requireNotNull(context.contentResolver.openInputStream(uri))) { !job.isActive }
                }
                "Resource verified. A complete matching pair is activated atomically; the previous trusted version is retained for rollback."
            }
            catch (e: CancellationException) { status = "Import cancelled. Verified partial resources can be reselected to resume. If cancellation arrived after the atomic commit, the complete pair may already be active; check readiness before retrying."; throw e }
            catch (_: Exception) { "Import refused. This build needs an independently verified, pinned release manifest; arbitrary model checksums are not trusted." }
            finally { busy = false }
        }
    }
    PageTitle("Local model", "Built with Llama · ExecuTorch · no cloud inference")
    Text(status)
    Text("Reference profile: Meta Llama 3.2 1B Instruct, ExecuTorch 1.2.0 CPU/XNNPACK. Only matching exported .pte and matching official tokenizer resources are accepted. Original Hugging Face weights cannot be loaded directly.")
    Text("This build pins the supported 1.14 GB SpinQuant export and 2.18 MB tokenizer. Import both files from the documented resource setup. Arbitrary exports fail verification. Allow at least 1.3 GB free private storage for installation, plus room for any previous version. Detection and curated guidance remain available without the model.")
    if (!busy) {
        TextAction("Import model export", { name = "model"; picker.launch(arrayOf("application/octet-stream")) })
        TextAction("Import matching official tokenizer", { name = "tokenizer"; picker.launch(arrayOf("application/octet-stream")) })
        TextAction("Test local inference", {
            importJob = scope.launch {
                busy = true
                try {
                    val started = android.os.SystemClock.elapsedRealtime()
                    val assessment = withContext(Dispatchers.Default) {
                        dev.kyzenn.nivoryn.domain.AnalyseInput().analyse(dev.kyzenn.nivoryn.domain.CheckDraft(text = "Please send your OTP to finish verification."))
                    }
                    val result = runner.explain(assessment)
                    status = "${result.explanationMode}\nElapsed: ${android.os.SystemClock.elapsedRealtime() - started} ms. This is one synthetic smoke check, not an accuracy or performance benchmark."
                } catch (e: CancellationException) { status = "Inference cancelled."; throw e }
                finally { busy = false }
            }
        })
        TextAction("Roll back to previous trusted version", {
            importJob = scope.launch {
                busy = true
                val job = currentCoroutineContext().job
                status = try { runner.manageResources { store.rollback { !job.isActive } }; "Previous trusted resources restored." }
                catch (e: CancellationException) { status = "Rollback cancelled. If cancellation arrived after the atomic commit, the previous pair may already be active. Check readiness before retrying."; throw e }
                catch (_: Exception) { "No complete, trusted previous version is available. Active resources were kept." }
                finally { busy = false }
            }
        })
        TextAction("Delete model resources", { confirmDelete = true })
    } else {
        Text("Working locally… Inference and resource changes share one runner.")
        TextAction("Cancel resource operation", { importJob?.cancel() })
    }
    if (confirmDelete) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Delete local models?") },
        text = { Text("Removes active, previous and partially imported model resources. Saved checks are kept. Detection will use curated fallback until a trusted pair is installed again.") },
        confirmButton = { TextAction("Delete models", {
            confirmDelete = false
            importJob = scope.launch {
                busy = true
                status = try { runner.manageResources { store.deleteAll() }; "Model resources deleted. Saved checks are unchanged." }
                catch (e: CancellationException) { status = "Deletion cancelled. Removal may already have finished; check readiness before assuming resources remain."; throw e }
                catch (_: Exception) { "Some resources could not be deleted. Retry before assuming deletion succeeded." }
                finally { busy = false }
            }
        }) }, dismissButton = { TextAction("Keep models", { confirmDelete = false }) })
    Text("Llama generates the explanation prose. Sources use validated model references, or local binding when exactly one known finding supplied the context. The result names that mode. Unknown claims/words, unsafe instructions, certainty and invalid references are rejected; remediation uses approved action IDs. Validation is bounded, not a semantic correctness guarantee. Rejected output uses labelled curated fallback.")
    TextAction("Llama licence and notice", { showLicence = true })
    if (showLicence) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showLicence = false }, title = { Text("Built with Llama") },
        text = { androidx.compose.foundation.text.selection.SelectionContainer {
            Text(licence.ifBlank { "Loading local licence…" }, Modifier.verticalScroll(rememberScrollState()))
        } }, confirmButton = { TextAction("Close licence", { showLicence = false }) })
}
