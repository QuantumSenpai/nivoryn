package dev.kyzenn.nivoryn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CoverageCheck(val name: String, val status: CoverageStatus, val detail: String, val evidence: List<String> = emptyList())
enum class CoverageStatus { COMPLETED, PARTIAL, UNAVAILABLE, FAILED }

@Composable fun CoveragePanel(checks: List<CoverageCheck>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CoverageSummary(checks)
        checks.forEach { check ->
            Disclosure("${check.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${check.name}", check.detail)
        }
    }
}
@Composable fun CoverageSummary(checks: List<CoverageCheck>) {
    Text("${checks.count { it.status == CoverageStatus.COMPLETED }} completed · ${checks.count { it.status == CoverageStatus.UNAVAILABLE }} unavailable",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
