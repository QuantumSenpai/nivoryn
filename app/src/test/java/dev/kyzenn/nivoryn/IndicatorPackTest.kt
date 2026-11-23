package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.detection.IndicatorPack
import dev.kyzenn.nivoryn.detection.UrlAnalyser
import org.junit.Assert.*
import org.junit.Test
class IndicatorPackTest {
    @Test fun expiryAndExactDomainBoundaryAreRespected() {
        val pack=IndicatorPack("fixture",1,10,100,setOf("bad.example"),emptySet(),emptySet())
        assertTrue(pack.matchesDomain("login.bad.example",50))
        assertFalse(pack.matchesDomain("notbad.example",50))
        assertFalse(pack.matchesDomain("bad.example",101))
        assertTrue(UrlAnalyser(pack,50).inspect("https://bad.example").evidence.any { it.title=="Local threat-list match" })
    }
    @Test fun brandMismatchDoesNotFlagTheOfficialDomain() {
        assertTrue(UrlAnalyser().inspect("https://paypal.com/account").evidence.isEmpty())
        assertTrue(UrlAnalyser().inspect("https://paypal.com.evil.example").evidence.any { it.title=="Brand / domain mismatch" })
    }
}
