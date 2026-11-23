package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.documents.DocumentInspector
import org.junit.Assert.*
import org.junit.Test
class DocumentInspectorTest {
    @Test fun comparesOnlyExplicitFieldsAcrossSources() {
        val findings = DocumentInspector.inspect("PNR: ABC123\nAmount: INR 1200\nDate: 2026-11-23", "PNR: XYZ999\nAmount: INR 1300\nDate: 2026-11-24")
        assertEquals(3, findings.size)
        assertTrue(findings.all { it.explanation.contains("not prove") })
    }
    @Test fun matchingDataDoesNotCertifyBookingOrSignature() {
        assertTrue(DocumentInspector.inspect("PNR: ABC123\nAmount: ₹1,200.00", "PNR: ABC123\nAmount: INR 1200").isEmpty())
        assertTrue(DocumentInspector.inspect("Departure: 2026-11-23\nReturn: 2026-11-25", "https://example.org").isEmpty())
    }
    @Test fun flagsInvalidCalendarDatesWithoutGuessingAmbiguousDates() {
        assertEquals(1, DocumentInspector.inspect("Date: 2026-02-30", "").size)
        assertTrue(DocumentInspector.inspect("Date: 03/04/2026", "").isEmpty())
    }
}
