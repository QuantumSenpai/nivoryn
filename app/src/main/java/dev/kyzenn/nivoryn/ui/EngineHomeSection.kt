package dev.kyzenn.nivoryn.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.data.*
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun EngineHomeSection(checks: List<SavedCheck>, onOpen: (SavedCheck) -> Unit, onPage: (String) -> Unit) {
    val context = LocalContext.current
    val ready by produceState<Boolean?>(null) {
        value = withContext(Dispatchers.IO) { ModelStore(context).installed() != null }
    }
    HorizontalDivider()
    SectionTitle("On this device")
    Text("Text rules, URL structure, bundled OCR and QR are available.")
    Text("English SMS spam-like wording: bundled statistical classifier. Hindi/Hinglish: explicit rules. Neither verifies sender identity.", style = MaterialTheme.typography.bodySmall)
    Text(when (ready) {
        true -> "Llama resources verified · inference readiness is established when a check runs."
        false -> "Llama resources missing · checks use labelled curated guidance."
        null -> "Checking model resources…"
    }, style = MaterialTheme.typography.bodySmall)
    TextAction("Manage local model", { onPage("model") })
    TextAction("Inspect network capture", { onPage("network") })
    TextAction("Inspect APK indicators", { onPage("apk") })
    if (checks.isNotEmpty()) {
        SectionTitle("Recent saved checks")
        checks.take(3).forEach { check -> TextAction(check.preview.take(70), { onOpen(check) }) }
    }
}
