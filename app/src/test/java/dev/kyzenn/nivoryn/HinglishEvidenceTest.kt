package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.detection.RuleEngine
import org.junit.Assert.*
import org.junit.Test
class HinglishEvidenceTest {
    @Test fun mapsObfuscatedHinglishToOriginalOffsets() {
        val text = "नमस्ते! apna O\u200bTP batao abhi"
        val evidence = RuleEngine().inspect(text).single().evidence
        assertEquals(evidence.quote, text.substring(evidence.start, evidence.end))
        assertTrue(evidence.quote.contains("O\u200bTP"))
    }
    @Test fun safetyAdviceIsNotASecretRequest() {
        assertTrue(RuleEngine().inspect("Never share your OTP").isEmpty())
    }
}
