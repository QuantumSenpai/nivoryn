package dev.kyzenn.nivoryn.detection

import dev.kyzenn.nivoryn.domain.Evidence
import java.text.Normalizer

data class RuleFinding(val evidence: Evidence, val high: Boolean)

class RuleEngine {
    private data class Rule(val title: String, val pattern: Regex, val explanation: String, val high: Boolean = true)
    private val rules = listOf(
        Rule("Job / refund fee condition", Regex("""\b(?:registration|training|processing) fee\b[^.!?\n]{0,50}\b(?:job|refund|reward)\b|\b(?:job|refund|reward)\b[^.!?\n]{0,50}\b(?:registration|training|processing) fee\b"""), "A fee is linked to a job, refund or reward. Verify the terms through a known official channel before paying.", false),
        Rule("KYC threat in Hinglish", Regex("""\bkyc\b[^.!?\n]{0,65}\b(?:account|khata)\b[^.!?\n]{0,30}\b(?:band|block)\b"""), "KYC language is paired with an account-closure threat. Verify through the institution's known app or contact.", false),
        Rule("डिजिटल अरेस्ट का दबाव", Regex("""(?:आप|तुम)[^।.!?\n]{0,35}डिजिटल अरेस्ट"""), "A claim of digital arrest is coercive language. Verify independently; do not pay the sender."),
        Rule("नौकरी के लिए शुल्क", Regex("""नौकरी[^।.!?\n]{0,40}(?:पंजीकरण|प्रशिक्षण)? शुल्क[^।.!?\n]{0,25}(?:भेजें|जमा करें|दें)"""), "नौकरी के लिए पहले शुल्क देने का अनुरोध मिला। स्वतंत्र रूप से पुष्टि करें।", false),
        Rule("Claimed official role", Regex("""\b(?:bank|police|cbi|rbi|government) (?:officer|support|agent)\b[^.!?\n]{0,70}\b(?:pay|send|install|share|verify)\b"""), "An official role is claimed alongside an action request. The claim does not establish the sender's identity.", false),
        Rule("Secret request in Hinglish", Regex("(?:otp|pin|password)\\b[^.!?\\n]{0,30}\\b(?:batao|bhejo|bata|share karo|de do)|\\b(?:batao|bhejo|share karo)\\b[^.!?\\n]{0,30}\\b(?:otp|pin|password)\\b"), "A secret is requested. Do not share an OTP, PIN or password."),
        Rule("Withdrawal fee pressure", Regex("\\b(?:withdrawal|withdraw|nikalne|nikaalne)\\b[^.!?\\n]{0,40}\\b(?:fee|fees|charge|paisa|paise)\\b"), "A fee linked to withdrawing promised funds deserves independent verification.", false),
        Rule("Secrecy request", Regex("\\b(?:keep (?:this|it) secret|do not tell anyone|kisi ko mat batana)\\b"), "Secrecy can prevent independent verification. Review the context with a trusted person.", false),
        Rule("Screen sharing request", Regex("\\b(?:share your screen|screen share karo|screen sharing)\\b"), "Screen sharing may expose private information. Verify who is asking before proceeding.", false),
        Rule("A secret is requested", Regex("\\b(?:share|send|give|reveal|tell|provide)\\b[^.!?\\n]{0,45}\\b(?:otp|pin|password|cvv|one.time password)\\b"), "This request could expose a credential that authorises account access or payment."),
        Rule("Upfront payment pressure", Regex("\\b(?:pay|send|transfer|deposit)\\b[^.!?\\n]{0,65}\\b(?:release|unlock|claim|receive)\\b[^.!?\\n]{0,40}\\b(?:earnings|salary|prize|reward|loan|winnings|job)\\b"), "Payment is being made a condition of receiving a promised benefit."),
        Rule("Threat of digital arrest", Regex("\\b(?:under|face|facing) (?:a )?digital arrest\\b"), "A digital-arrest claim is coercive language. Independently contact the relevant authority; do not pay the sender."),
        Rule("Remote-control installation request", Regex("\\b(?:install|download)\\b[^.!?\\n]{0,40}\\b(?:anydesk|teamviewer|remote access)\\b"), "Remote-access software can expose your device to another person. Verify the request independently.", false),
        Rule("Urgent account threat", Regex("\\b(?:account|kyc|card)\\b[^.!?\\n]{0,45}\\b(?:blocked|suspended|deactivated)\\b[^.!?\\n]{0,25}\\b(?:today|now|immediately)\\b"), "An account threat paired with urgency deserves independent verification.", false),
        Rule("गोपनीय जानकारी की माँग", Regex("(?:भेजें|बताएं|बताएँ|साझा करें)[^।.!?\\n]{0,35}(?:ओटीपी|पिन|पासवर्ड)|(?:ओटीपी|पिन|पासवर्ड)[^।.!?\\n]{0,25}(?:भेजें|बताएं|बताएँ|साझा करें)"), "OTP, PIN या पासवर्ड साझा करने का अनुरोध मिला।"),
        Rule("पहले भुगतान का दबाव", Regex("(?:पैसे|शुल्क|जमा राशि)[^।.!?\\n]{0,35}(?:भेजें|जमा करें)[^।.!?\\n]{0,35}(?:कमाई|इनाम|नौकरी)"), "भुगतान को कमाई या इनाम पाने की शर्त बनाया जा रहा है।")
    )
    /** Lookup only authored rule text; never forward quotes or interpolated URL details to an LLM. */
    fun advisoryFact(title: String): String? {
        val canonical = when (title) {
            "Secret request in Hinglish", "गोपनीय जानकारी की माँग" -> "A secret is requested"
            "पहले भुगतान का दबाव" -> "Upfront payment pressure"
            "डिजिटल अरेस्ट का दबाव" -> "Threat of digital arrest"
            "नौकरी के लिए शुल्क" -> "Job / refund fee condition"
            else -> title
        }
        return rules.find { it.title == canonical }?.explanation?.substringBefore('.')
    }
    fun inspect(original: String): List<RuleFinding> {
        val positions = mutableListOf<Int>()
        val normalized = buildString {
            original.forEachIndexed { index, char ->
                if (char !in "\u200b\u200c\u200d\ufeff\u2060") {
                    val part = Normalizer.normalize(char.toString(), Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
                    append(part); repeat(part.length) { positions.add(index) }
                }
            }
        }
        return rules.mapNotNull { rule ->
            rule.pattern.findAll(normalized).firstOrNull { match ->
                val prefix = normalized.substring((match.range.first - 35).coerceAtLeast(0), match.range.first)
                val negatedBefore = Regex("""(?:\bnever|\bdo not|\bdon.t|\bnot to|\bmat|मत|नहीं)\s*$""").containsMatchIn(prefix)
                val secretRule = rule.title in listOf("A secret is requested", "Secret request in Hinglish", "गोपनीय जानकारी की माँग")
                val negatedWithin = secretRule && Regex("""(?:\bmat|मत|नहीं)\s+(?:batao|bhejo|bata|share karo|देना|भेजें|बताएं|बताएँ|साझा करें)""").containsMatchIn(match.value)
                !negatedBefore && !negatedWithin
            }?.let { match ->
                RuleFinding(Evidence(rule.title, original.substring(positions[match.range.first], positions[match.range.last] + 1), rule.explanation, positions[match.range.first], positions[match.range.last] + 1), rule.high)
            }
        }
    }
}
