package dev.kyzenn.nivoryn.domain

import dev.kyzenn.nivoryn.detection.*

/** Local, bounded analysis. Input never becomes an instruction, URL request or executable code. */
class AnalyseInput(private val pack: IndicatorPack? = null, private val classifier: SpamClassifier? = null) {
    fun analyse(draft: CheckDraft): DemoResult {
        val text = draft.text
        val unavailable = listOf("Live reputation and identity checks", "ML scam classifier and Llama inference", "File authenticity, malware scan and transcription")
        val unverified = listOf("Sender and recipient ownership", "Current account, website and booking status")
        fun result(level: Assessment, explanation: String, evidence: List<Evidence>, completed: List<String>, steps: List<String>) =
            DemoResult(level, explanation, "स्थानीय नियमों का सीमित आकलन। मूल साक्ष्य और अप्रमाणित बातें नीचे पढ़ें। यह सुरक्षा की गारंटी नहीं है।", evidence, completed, unavailable, unverified, steps, ResultOrigin.LOCAL)
        if ((text.isBlank() && draft.qrPayload.isBlank()) || text.length > 12000 || draft.qrPayload.length > 12000 || draft.kind in listOf("Audio", "APK")) {
            return result(Assessment.UNKNOWN, "There is not enough supported text to assess. Add or correct the text; audio and APK analysis are unavailable.", emptyList(), emptyList(), listOf("Review the original content. Do not infer safety from an unavailable check."))
        }
        val qrSource = "Decoded QR" + draft.extractionSource.takeIf { it.isNotBlank() }?.let { " · ${it.take(180)}" }.orEmpty()
        val sameQr = draft.qrPayload.isNotBlank() && draft.qrPayload == text
        val textSource = when {
            draft.kind == "QR" && sameQr -> qrSource
            draft.kind == "QR" -> "Reviewed QR text (editable)"
            else -> draft.extractionSource.ifBlank { "Reviewed text" }.take(200)
        }
        val rules = RuleEngine().inspect(text).map { it.copy(evidence = it.evidence.copy(source = textSource)) } +
            if (draft.qrPayload.isNotBlank() && draft.qrPayload != text) RuleEngine().inspect(draft.qrPayload).map { it.copy(evidence = it.evidence.copy(source = qrSource)) } else emptyList()
        val urls = UrlAnalyser(pack).inspect(text, draft.kind == "Link" || draft.kind == "QR")
        val qrUrls = if (draft.qrPayload.isNotBlank() && !sameQr) UrlAnalyser(pack).inspect(draft.qrPayload, draft.kind == "QR" || text.isBlank()) else null
        val document = if (draft.kind == "Document" && text.isNotBlank()) dev.kyzenn.nivoryn.documents.DocumentInspector.inspect(text, draft.qrPayload).map { it.copy(source = "Reviewed document / decoded QR") } else emptyList()
        val classifierRan = classifier != null && draft.kind in listOf("Text", "Screenshot") && classifier.supports(text)
        val statistical = if (classifierRan) listOfNotNull(classifier?.evidence(text)) else emptyList()
        val allEvidence = (statistical.map { it.copy(source = textSource) } + document + rules.map { it.evidence } + urls.evidence.map { it.copy(source = textSource) } + qrUrls?.evidence.orEmpty().map { it.copy(source = qrSource) }).distinctBy { Triple(it.title, it.quote, it.source) }
        val evidence = allEvidence.take(AnalysisLimits.EVIDENCE)
        val invalid = urls.invalid || qrUrls?.invalid == true
        val examined = urls.examined || qrUrls?.examined == true
        val level = when {
            rules.any { it.high } -> Assessment.HIGH
            evidence.isNotEmpty() -> Assessment.SUSPICIOUS
            maxOf(text.trim().length, draft.qrPayload.trim().length) < 12 && !examined -> Assessment.UNKNOWN
            invalid -> Assessment.UNKNOWN
            else -> Assessment.NONE
        }
        val explanation = when (level) {
            Assessment.HIGH -> "A specific high-risk request was found by local rules. Read the quoted evidence before acting. Sender identity is still unverified."
            Assessment.SUSPICIOUS -> "Local checks found details worth verifying. These signals do not prove fraud or establish who controls the destination."
            Assessment.NONE -> "No clear warning was found by these limited local checks. This is not a guarantee of safety; new or differently worded scams can be missed."
            Assessment.UNKNOWN -> if (invalid) "The destination is malformed or unsupported. No safety assessment can be made from it." else "There is too little context for a useful assessment. Add the relevant request without sharing credentials."
        }
        val completed = listOf("Local text rules v2", if (pack == null) "No threat pack installed; no threat-feed lookup ran" else "Indicator pack ${pack.version} · ${pack.source} · expires ${pack.expires} · ${if (pack.fresh(System.currentTimeMillis())) "current" else "stale; matches disabled"}") +
            (if (draft.kind == "Document" && text.isNotBlank()) listOf("Labelled document field / decoded QR consistency; issuer and signatures unverified") else emptyList()) +
            (if (text.isBlank()) listOf("Decoded QR only. No OCR text was extracted or compared.") else emptyList()) +
            (if (examined) listOf("Offline URL / payment-payload structure") else emptyList()) +
            (if (allEvidence.size > evidence.size) listOf("Evidence display capped at ${AnalysisLimits.EVIDENCE}; ${allEvidence.size - evidence.size} additional indicators omitted. Review the full original input.") else emptyList())
        return result(level, explanation, evidence, completed,
            listOf(if (level == Assessment.HIGH) "Pause any payment or secret-sharing request. Verify through an official contact you already know." else "Verify unexpected requests independently using a known app or contact.", "Never share an OTP, PIN or password. No link was opened and no payment was initiated.")).let { if (classifierRan) it.copy(completed = it.completed + "Local historical-English SMS spam proxy ${classifier?.version}", unavailable = it.unavailable.map { item -> if (item == "ML scam classifier and Llama inference") "Llama inference" else item }) else it }.copy(destinations = (urls.destinations + qrUrls?.destinations.orEmpty()).distinct().take(AnalysisLimits.DESTINATIONS))
    }
}
