package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.domain.*
import org.junit.Assert.*
import org.junit.Test
class ScamCoverageTest {
    @Test fun explicitEnglishHindiAndHinglishScamRequestsHaveEvidence() {
        listOf("Pay a registration fee for this job", "Refund processing fee must be paid first", "We are bank support. Install AnyDesk now",
            "KYC abhi karo warna account band ho jayega", "आप डिजिटल अरेस्ट में हैं पैसे भेजें", "नौकरी के लिए पंजीकरण शुल्क भेजें").forEach { text ->
            val result=AnalyseInput().analyse(CheckDraft(text=text))
            assertTrue(text,result.evidence.isNotEmpty())
            assertTrue(result.evidence.all { text.contains(it.quote) })
        }
    }
    @Test fun secretSafetyAdviceInHindiAndHinglishIsNotARequest() {
        listOf("अपना ओटीपी मत भेजें", "otp mat batao", "mat share karo otp").forEach { text ->
            assertTrue(text,AnalyseInput().analyse(CheckDraft(text=text)).evidence.isEmpty())
        }
    }
}
