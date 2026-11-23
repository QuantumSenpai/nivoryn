package dev.kyzenn.nivoryn.reasoning

import dev.kyzenn.nivoryn.domain.DemoResult
import org.json.JSONObject
import java.util.Locale

enum class SourceBinding { MODEL_REFERENCES, LOCAL_SINGLE_FINDING, NO_FINDINGS }
data class ValidatedGuidance(val explanation: String, val actions: List<String>, val binding: SourceBinding = SourceBinding.MODEL_REFERENCES)

/** Grounding/lexical/output checks, not a claim of complete semantic verification. */
object ExplanationValidator {
    private val words = Regex("[a-z]+")
    private val connective = "a an the this these those it its is are was were be been has have had to of for from in on by with and or but as at that which their your our local check checks finding findings evidence signal signals risk risks warning warnings request requests review may might could can would should not no only also because detail details information account accounts independent independently indicates suggests found wording concerns context remains remain unverified"
    fun validate(output: String, result: DemoResult): ValidatedGuidance? = try {
        val clean = output.trim().removeSuffix("<|eot_id|>").trim()
        require(clean.length in 10..2048)
        require(clean.none { it.code < 32 && it !in "\n\r\t" })
        val parser = org.json.JSONTokener(clean)
        val j = parser.nextValue() as? JSONObject ?: error("Expected a JSON object")
        require(parser.nextClean() == '\u0000' && !parser.more())
        require(j.keys().asSequence().toSet() == setOf("summary", "actions"))
        val generated = j.getString("summary").trim()
        val localBinding = result.evidence.size == 1 && AdvisoryFacts.known(result.evidence.single().title) &&
            !generated.contains('[') && !generated.contains(']')
        // The caller supplied exactly one controlled finding. Bind its source locally, then
        // run every content/lexical/negation/action check below. No prose is substituted.
        val summary = if (localBinding) generated.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.joinToString(" ") { sentence ->
            val punctuation = sentence.lastOrNull()?.takeIf { it in ".!?" }
            if (punctuation != null) sentence.dropLast(1) + " [E1]" + punctuation else "$sentence [E1]."
        } else generated
        require(summary.length in 10..600 && summary.all { it.code in 32..126 && it !in "<>`{}\\" })
        val lower = summary.lowercase(Locale.ROOT)
        if (result.evidence.isNotEmpty()) require(!Regex("\\b(?:not|never)\\s+(?:expose|exposes|exposing|authorise|authorize|a risk)\\b|\\bno\\s+(?:risk|warning|threat)\\b").containsMatchIn(lower))
        require(!Regex("https?://|www\\.|@|\\+?[0-9][0-9 ()-]{4,}|[0-9]+\\.[0-9]+\\.[0-9]+").containsMatchIn(lower))
        // Generated summary describes findings; all imperatives/remediation come from approved IDs.
        require(!Regex("\\b(pay|send|share|give|install|download|run|execute|open|call|contact|transfer|provide|reveal|tell)\\b").containsMatchIn(lower))
        require(!Regex("\\b(guaranteed?|definitely|certainly|confirmed|authentic|genuine|stolen|infected|compromised|criminal|hacker|attacker)\\b").containsMatchIn(lower))
        require(!Regex("\\b(is|are|proves?|means)\\s+(?:a\\s+)?(?:fraud|scam|malware|phishing|safe|legitimate)\\b").containsMatchIn(lower))
        val citations = Regex("\\[E([1-9][0-9]?)\\]").findAll(summary).toList()
        val indices = citations.map { it.groupValues[1].toInt() - 1 }.distinct()
        require(indices.all { it in result.evidence.take(8).indices })
        require(result.evidence.isEmpty() || citations.isNotEmpty())
        if (result.evidence.isNotEmpty()) {
            summary.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.forEach { sentence ->
                require(Regex("\\[E[1-9][0-9]?\\][.!?]?$").containsMatchIn(sentence))
                val refs = Regex("\\[E([1-9][0-9]?)\\]").findAll(sentence).map { it.groupValues[1].toInt() - 1 }.toList()
                val facts = refs.joinToString(" ") { AdvisoryFacts.describe(result.evidence[it].title) }
                val localWords = words.findAll((facts + " " + connective).lowercase(Locale.ROOT)).map { it.value }.toSet()
                val localProse = sentence.replace(Regex("\\[E[0-9]+\\]"), "")
                require(words.findAll(localProse.lowercase(Locale.ROOT)).all { it.value in localWords })
            }
        }
        val prose = summary.replace(Regex("\\[E[0-9]+\\]"), "")
        require(prose.none { it.isDigit() || it in "[]" })
        val grounded = if (indices.isEmpty()) AdvisoryFacts.NO_EVIDENCE else indices.joinToString(" ") { AdvisoryFacts.describe(result.evidence[it].title) }
        val allowed = words.findAll((grounded + " " + connective).lowercase(Locale.ROOT)).map { it.value }.toSet()
        require(words.findAll(prose.lowercase(Locale.ROOT)).all { it.value in allowed })
        require(Regex("\\b(may|might|could|can|risk|signal|warning|request|requested|requests)\\b").containsMatchIn(lower))
        val actions = j.getJSONArray("actions"); require(actions.length() in 1..6)
        val approved = List(actions.length()) {
            val ref = actions.getString(it); require(ref.matches(Regex("A[1-6]")))
            requireNotNull(result.nextSteps.take(6).getOrNull(ref.drop(1).toInt() - 1))
        }.distinct()
        val binding = when { localBinding -> SourceBinding.LOCAL_SINGLE_FINDING; result.evidence.isEmpty() -> SourceBinding.NO_FINDINGS; else -> SourceBinding.MODEL_REFERENCES }
        ValidatedGuidance(summary + " These signals do not prove fraud. Identity and live status remain unverified.", approved, binding)
    } catch (_: Exception) { null }
}
