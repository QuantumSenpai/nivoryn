package dev.kyzenn.nivoryn.detection

import dev.kyzenn.nivoryn.domain.Evidence
import java.text.Normalizer

data class RuleFinding(val evidence: Evidence, val high: Boolean)

class RuleEngine {
    private data class Rule(val title: String, val pattern: Regex, val explanation: String, val high: Boolean = true)
    private val rules = listOf(
        Rule("A secret is requested", Regex("\\b(?:share|send|give|reveal|tell|provide)\\b[^.!?\\n]{0,45}\\b(?:otp|pin|password|cvv|one.time password)\\b"), "This request could expose a credential that authorises account access or payment."),
        Rule("Upfront payment pressure", Regex("\\b(?:pay|send|transfer|deposit)\\b[^.!?\\n]{0,65}\\b(?:release|unlock|claim|receive)\\b[^.!?\\n]{0,40}\\b(?:earnings|salary|prize|reward|loan|winnings|job)\\b"), "Payment is being made a condition of receiving a promised benefit."),
        Rule("Threat of digital arrest", Regex("\\b(?:under|face|facing) (?:a )?digital arrest\\b"), "A digital-arrest claim is coercive language. Independently contact the relevant authority; do not pay the sender."),
        Rule("Remote-control installation request", Regex("\\b(?:install|download)\\b[^.!?\\n]{0,40}\\b(?:anydesk|teamviewer|remote access)\\b"), "Remote-access software can expose your device to another person. Verify the request independently.", false),
        Rule("Urgent account threat", Regex("\\b(?:account|kyc|card)\\b[^.!?\\n]{0,45}\\b(?:blocked|suspended|deactivated)\\b[^.!?\\n]{0,25}\\b(?:today|now|immediately)\\b"), "An account threat paired with urgency deserves independent verification.", false),
        Rule("गोपनीय जानकारी की माँग", Regex("(?:भेजें|बताएं|बताएँ|साझा करें)[^।.!?\\n]{0,35}(?:ओटीपी|पिन|पासवर्ड)|(?:ओटीपी|पिन|पासवर्ड)[^।.!?\\n]{0,25}(?:भेजें|बताएं|बताएँ|साझा करें)"), "OTP, PIN या पासवर्ड साझा करने का अनुरोध मिला।"),
        Rule("पहले भुगतान का दबाव", Regex("(?:पैसे|शुल्क|जमा राशि)[^।.!?\\n]{0,35}(?:भेजें|जमा करें)[^।.!?\\n]{0,35}(?:कमाई|इनाम|नौकरी)"), "भुगतान को कमाई या इनाम पाने की शर्त बनाया जा रहा है।")
    )
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
                !Regex("(?:\\bnever|\\bdo not|\\bdon.t|\\bnot to|मत|नहीं)\\s*$").containsMatchIn(prefix)
            }?.let { match ->
                RuleFinding(Evidence(rule.title, original.substring(positions[match.range.first], positions[match.range.last] + 1), rule.explanation), rule.high)
            }
        }
    }
}
