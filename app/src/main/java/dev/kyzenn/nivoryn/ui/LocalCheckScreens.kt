package dev.kyzenn.nivoryn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.data.*
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun LocalProcessingScreen(draft: CheckDraft, hi: Boolean, active: Boolean, onComplete: (DemoResult, Long) -> Unit, onCancel: () -> Unit, requestToken: Long) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as dev.kyzenn.nivoryn.NivorynApplication
    val complete by rememberUpdatedState(onComplete)
    LaunchedEffect(draft, active, requestToken) {
        if (active) {
            val result = FrontendDemoRepository().resultFor(draft)
                ?: application.llama.explain(withContext(Dispatchers.Default) { AnalyseInput(IndicatorStore(context).load(), application.spam).analyse(draft) })
            complete(result, requestToken)
        }
    }
    PageTitle(if (hi) "फ़ोन पर जाँच" else "Checking on this device")
    Text(if (hi) "कोई सामग्री अपलोड नहीं होती।" else "Your content stays here. No network request is made.", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    LinearProgressIndicator(Modifier.fillMaxWidth())
    TextAction(if (hi) "रद्द करें" else "Cancel", onCancel)
}

@Composable fun SavedChecksSection(checks: List<SavedCheck>, unavailable: Boolean, hi: Boolean, onOpen: (SavedCheck) -> Unit, onDelete: (String) -> Unit) {
    PageTitle(if (hi) "सहेजी गई जाँच" else "Your saved checks", if (hi) "केवल आपकी पसंद से, केवल इस फ़ोन पर।" else "Saved deliberately. Encrypted on this device.")
    if (unavailable) Notice("Saved checks could not be unlocked. They have not been overwritten. You can clear them in Settings.")
    if (checks.isEmpty() && !unavailable) Text(if (hi) "अभी कोई जाँच सहेजी नहीं गई।" else "Nothing saved yet. Save a result when you want to keep its original text.")
    checks.forEach { check ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextAction(check.preview.take(85), { onOpen(check) })
            Text("${check.kind} · ${java.text.DateFormat.getDateInstance().format(java.util.Date(check.created))}", style = MaterialTheme.typography.bodySmall)
            TextAction(if (hi) "सहेजी हुई जाँच हटाएँ" else "Delete saved check", { onDelete(check.id) })
            HorizontalDivider()
        }
    }
    Text(if (hi) "सहेजा पाठ 30 दिन बाद हटाया जाता है। अधिकतम 50 जाँच।" else "Saved text expires after 30 days when the app opens. Up to 50 checks; original files are never copied.", style = MaterialTheme.typography.bodySmall)
}
