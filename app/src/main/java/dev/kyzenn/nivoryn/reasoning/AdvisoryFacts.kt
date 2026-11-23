package dev.kyzenn.nivoryn.reasoning

import dev.kyzenn.nivoryn.detection.RuleEngine

/** Authored fact catalogue. Never derives prompt vocabulary from interpolated file/URL data. */
object AdvisoryFacts {
    const val NO_EVIDENCE = "No clear warning found by limited local checks; safety is unverified."
    private const val UNKNOWN = "The local finding remains unverified and needs independent review."
    private val rules = RuleEngine()
    private val facts = mapOf(
        "Outgoing payment request" to "The payload requests a payment. Recipient ownership and the reason for payment remain unverified.",
        "Local threat-list match" to "The destination matches a current signed local indicator pack. List membership is evidence; ownership and current content remain unverified.",
        "Brand / domain mismatch" to "The hostname contains a supported brand name but is outside its configured domains. This is a limited heuristic, not proof of phishing.",
        "Nested destination" to "The query contains an encoded or nested URL. The visible host does not establish the eventual destination. Redirects were not followed.",
        "Misleading URL authority" to "Embedded user information can obscure the actual destination. The apparent name does not establish destination ownership.",
        "Unencrypted destination" to "HTTP does not encrypt the connection. HTTPS alone would not establish trust either.",
        "Numeric destination" to "The URL uses a numeric address. Local checks do not contact this address or establish its owner.",
        "Internationalised host" to "The host uses internationalised characters. This alone does not prove fraud.",
        "Layered hostname" to "Several subdomains can conceal the destination. No live ownership check ran.",
        "Spam-like wording (local classifier)" to "A local statistical classifier trained on historical English SMS found spam-like wording. Spam is not proof of fraud; sender identity and multilingual accuracy remain unverified.",
        "Repeated document field differs" to "The same labelled field has multiple values. Multiple passengers or OCR errors may explain it; this does not prove forgery.",
        "Document / QR field mismatch" to "Reviewed text and decoded QR disagree on the same labelled field. Extraction errors may explain the mismatch. A mismatch does not prove fraud.",
        "Invalid calendar date" to "A labelled date is not a valid calendar day. OCR or data entry errors are possible; this does not prove forgery."
    )
    fun describe(title: String): String = rules.advisoryFact(title)?.let { "$title. $it" }
        ?: facts[title]?.let { "$title. $it" } ?: UNKNOWN
    fun known(title: String): Boolean = rules.advisoryFact(title) != null || title in facts
}
