package dev.kyzenn.nivoryn.domain

import dev.kyzenn.nivoryn.detection.*

/** Local, bounded analysis. Input never becomes an instruction, URL request or executable code. */
class AnalyseInput {
    fun analyse(draft: CheckDraft): DemoResult {
        val text = draft.text
        val unavailable = listOf("Live reputation and identity checks", "ML scam classifier and Llama inference", "File authenticity, malware scan and transcription")
        val unverified = listOf("Sender and recipient ownership", "Current account, website and booking status")
        fun result(level: Assessment, explanation: String, evidence: List<Evidence>, completed: List<String>, steps: List<String>) =
            DemoResult(level, explanation, "स्थानीय नियमों का सीमित आकलन। मूल साक्ष्य और अप्रमाणित बातें नीचे पढ़ें। यह सुरक्षा की गारंटी नहीं है।", evidence, completed, unavailable, unverified, steps, ResultOrigin.LOCAL)
        if (text.isBlank() || text.length > 12000 || draft.kind in listOf("Audio", "APK")) {
            return result(Assessment.UNKNOWN, "There is not enough supported text to assess. Add or correct the text; audio and APK analysis are unavailable.", emptyList(), emptyList(), listOf("Review the original content. Do not infer safety from an unavailable check."))
        }
        val rules = RuleEngine().inspect(text)
        val urls = UrlAnalyser().inspect(text, draft.kind == "Link" || draft.kind == "QR")
        val evidence = (rules.map { it.evidence } + urls.evidence).distinctBy { it.title to it.quote }
        val level = when {
            rules.any { it.high } -> Assessment.HIGH
            evidence.isNotEmpty() -> Assessment.SUSPICIOUS
            text.trim().length < 12 && !urls.examined -> Assessment.UNKNOWN
            urls.invalid -> Assessment.UNKNOWN
            else -> Assessment.NONE
        }
        val explanation = when (level) {
            Assessment.HIGH -> "A specific high-risk request was found by local rules. Read the quoted evidence before acting. Sender identity is still unverified."
            Assessment.SUSPICIOUS -> "Local checks found details worth verifying. These signals do not prove fraud or establish who controls the destination."
            Assessment.NONE -> "No clear warning was found by these limited local checks. This is not a guarantee of safety; new or differently worded scams can be missed."
            Assessment.UNKNOWN -> if (urls.invalid) "The destination is malformed or unsupported. No safety assessment can be made from it." else "There is too little context for a useful assessment. Add the relevant request without sharing credentials."
        }
        return result(level, explanation, evidence, listOf("Local text rules v1") + if (urls.examined) listOf("Offline URL / payment-payload structure") else emptyList(),
            listOf(if (level == Assessment.HIGH) "Pause any payment or secret-sharing request. Verify through an official contact you already know." else "Verify unexpected requests independently using a known app or contact.", "Never share an OTP, PIN or password. No link was opened and no payment was initiated."))
    }
}
