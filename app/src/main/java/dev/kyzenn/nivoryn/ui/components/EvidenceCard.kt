package dev.kyzenn.nivoryn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.domain.Evidence

@Composable fun EvidenceCard(evidence: Evidence, draftText: String = "", modifier: Modifier = Modifier, initiallyOpen: Boolean = false) {
    Column(modifier.fillMaxWidth()) {
        Disclosure(evidence.title, "“${evidence.quote}”\n\n${evidence.explanation}", initiallyOpen)
    }
}
@Composable fun EvidenceList(evidence: List<Evidence>, draftText: String = "", modifier: Modifier = Modifier, maxInitiallyOpen: Int = 1) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        evidence.forEachIndexed { i, item -> EvidenceCard(item, draftText, initiallyOpen = i < maxInitiallyOpen) }
        if (evidence.isEmpty()) Notice("No warning evidence from the available checks. This does not establish safety.")
    }
}
