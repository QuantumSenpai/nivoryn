package dev.kyzenn.nivoryn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.ui.components.*
import dev.kyzenn.nivoryn.ui.theme.*

@Composable
fun ResultScreen(
    result: DemoResult?,
    hi: Boolean,
    onSave: () -> Unit,
    onSamples: () -> Unit,
    onEdit: () -> Unit,
    draft: CheckDraft? = null,
    onDelete: () -> Unit = {}
) {
    val reduced = LocalReducedMotion.current

    if (result == null) {
        ResultEmptyState(hi, onEdit, onSamples)
        return
    }

    val color = when (result.assessment) {
        Assessment.HIGH -> MaterialTheme.colorScheme.error
        Assessment.SUSPICIOUS -> Warning
        Assessment.NONE -> Positive
        else -> MaterialTheme.colorScheme.onSurface
    }

    val label = if (hi) result.assessment.hindi else result.assessment.label

    var showEvidence by rememberSaveable { mutableStateOf(false) }
    var showInput by rememberSaveable { mutableStateOf(false) }
    var showCoverage by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Notice(if (result.origin == ResultOrigin.AUTHORED) {
            if (hi) "उदाहरण परिणाम · लिखे हुए डेमो डेटा" else "Example result · Authored demo data"
        } else if (hi) "स्थानीय जाँच · नियम आधारित, AI का फैसला नहीं" else "Local check · Rule-based, not an AI verdict")
        Text(if (result.origin == ResultOrigin.AUTHORED) "Mode: authored example. No detection engine ran." else "Mode: local rules v1. No network or AI inference.", style = MaterialTheme.typography.bodySmall)

        CrossFadeText(
            text = "!  $label",
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading(); liveRegion = LiveRegionMode.Polite }
        )

        CrossFadeText(
            text = if (hi) result.explanationHi else result.explanation,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        TextAction(
            label = if (showEvidence) (if (hi) "साक्ष्य छिपाएँ" else "Hide evidence") else (if (hi) "साक्ष्य देखें" else "View evidence"),
            onClick = { showEvidence = !showEvidence },
            modifier = Modifier.padding(top = 4.dp)
        )

        AnimatedVisibility(
            visible = showEvidence,
            enter = expandVertically(tween(if (reduced) 0 else 240, easing = EaseOutCustom)) + fadeIn(tween(if (reduced) 0 else 180, easing = EaseOutCustom)),
            exit = shrinkVertically(tween(if (reduced) 0 else 200, easing = EaseOutCustom)) + fadeOut(tween(if (reduced) 0 else 150, easing = EaseOutCustom))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(if (hi) "समर्थन में साक्ष्य" else "The supporting evidence")
                EvidenceList(
                    evidence = result.evidence,
                    draftText = draft?.text ?: ""
                )
            }
        }

        SectionTitle(if (hi) "अगला कदम" else "What to do next")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            result.nextSteps.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }

        if (draft != null && draft.text.isNotEmpty()) {
            SectionTitle(if (hi) "मूल इनपुट" else "Original input")
            val quote = result.evidence.firstOrNull()?.quote.orEmpty()
            val start = if (quote.isNotEmpty()) draft.text.indexOf(quote) else -1
            val annotated = buildAnnotatedString {
                if (start >= 0) {
                    append(draft.text.substring(0, start))
                    withStyle(SpanStyle(background = WarningPaper, color = Ink)) { append(quote) }
                    append(draft.text.substring(start + quote.length))
                } else append(draft.text)
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(annotated, style = MaterialTheme.typography.bodyLarge, maxLines = if (showInput) Int.MAX_VALUE else 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(20.dp))
                }
            }
        }

        if (draft != null && draft.text.isNotEmpty()) TextAction(
            if (showInput) (if (hi) "कम दिखाएँ" else "Show less input") else (if (hi) "पूरा इनपुट देखें" else "Show full input"), { showInput = !showInput })
        TextAction(if (showCoverage) (if (hi) "जाँच विवरण छिपाएँ" else "Hide check details") else (if (hi) "जाँच विवरण देखें" else "View check details"), { showCoverage = !showCoverage })
        AnimatedVisibility(showCoverage, enter = fadeIn(tween(if (reduced) 0 else 180)), exit = fadeOut(tween(if (reduced) 0 else 120))) {

        CoveragePanel(
            checks = result.completed.map { CoverageCheck(it, CoverageStatus.COMPLETED, "Completed for this result; see evidence and limitations.") } +
                result.unavailable.map { CoverageCheck(it, CoverageStatus.UNAVAILABLE, "This check did not run. Do not treat its absence as evidence of safety.") }

        )

        }

        Disclosure(
            title = if (hi) "क्या अप्रमाणित है" else "What remains unverified",
            text = result.unverified.joinToString("\n\n"),
            initiallyOpen = false
        )

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(
                label = if (hi) "डेमो केस में सहेजें" else if (result.origin == ResultOrigin.AUTHORED) "Save example to a case" else "Save check locally",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = result.origin == ResultOrigin.AUTHORED || (draft?.text?.isNotBlank() == true && draft.kind !in listOf("Audio", "APK"))
            )
            ActionButton(
                label = if (hi) "फिर जाँचें" else "Check again",
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                secondary = true
            )
            TextAction(
                label = if (hi) "दूसरा उदाहरण देखें" else "Explore another example",
                onClick = onSamples
            )
            if (draft != null) {
                TextAction(
                    label = if (hi) "इनपुट हटाएँ" else "Delete input",
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ResultEmptyState(hi: Boolean, onEdit: () -> Unit, onSamples: () -> Unit) {
    PageTitle(if (hi) "आकलन संभव नहीं" else "Cannot assess")
    Notice(if (hi) "इस इनपुट का परिणाम उपलब्ध नहीं है। वापस जाकर समीक्षा करें और स्थानीय जाँच चलाएँ।" else "No result is available for this input. Review the draft and run a local check.")
    Text(if (hi) "ड्राफ़्ट वापस जाकर संपादित करें या तैयार उदाहरण देखें।" else "Your draft is preserved for editing. Explore a predefined example to see how evidence and next steps will work.")
    ActionButton(label = if (hi) "ड्राफ़्ट पर वापस जाएँ" else "Return to draft", onClick = onEdit, modifier = Modifier.fillMaxWidth())
    ActionButton(label = if (hi) "उदाहरण देखें" else "Explore examples", onClick = onSamples, modifier = Modifier.fillMaxWidth(), secondary = true)
}

/** Reading text stays stable; the page transition owns entry and exit. */
@Composable
fun CrossFadeText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    Text(text, style = style, color = color, modifier = modifier)
}
