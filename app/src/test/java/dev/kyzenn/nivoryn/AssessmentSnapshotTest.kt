package dev.kyzenn.nivoryn

import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.data.*
import dev.kyzenn.nivoryn.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class AssessmentSnapshotTest {
    @Test fun preservesExactEvidenceAndExplanationModeAcrossReopen() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = CheckHistoryStore(ApplicationProvider.getApplicationContext()) { key }
        val result = AnalyseInput().analyse(CheckDraft(text="Please send your OTP"))
            .copy(explanationMode="Curated fallback · test")
        store.save("Please send your OTP", "Text", result = result)
        assertEquals(result, store.read().single().result)
        assertEquals(result, ResultCodec.decode(ResultCodec.encode(result)))
        val id=store.read().single().id
        store.save("Please send your OTP", "Text", result = result)
        assertEquals("Resaving an open result must preserve its ID",id,store.read().single().id)
    }
    @Test fun invalidSnapshotDoesNotMasqueradeAsASavedAssessment() {
        assertThrows(Exception::class.java) { ResultCodec.decode(org.json.JSONObject("{\"assessment\":\"SAFE\"}")) }
    }
    @Test fun denseSupportedUrlFindingsCanBeSavedAndReopened() {
        val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store=CheckHistoryStore(ApplicationProvider.getApplicationContext()) { key }
        val text=(0..15).joinToString(" ") { "http://support@a.b.c.paypal-$it.example/?next=https://example.com" }
        val qr=(16..31).joinToString(" ") { "http://support@a.b.c.paypal-$it.example/?next=https://example.com" }
        val result=AnalyseInput().analyse(CheckDraft(text=text,kind="Document",qrPayload=qr))
        store.save(text,"Document",result=result,qrPayload=qr)
        assertEquals(result,store.read().single().result)
        assertEquals(qr,store.read().single().qrPayload)
    }
    @Test fun qrOnlyDocumentPreservesItsSourceAndActualEvidence() {
        val key=KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store=CheckHistoryStore(ApplicationProvider.getApplicationContext()) { key }
        val qr="https://paypal.com@evil.example/verify"
        val draft=CheckDraft(kind="Document",qrPayload=qr,extractionSource="PDF page 1")
        assertTrue(draft.canReview)
        val result=AnalyseInput().analyse(draft)
        assertEquals(Assessment.SUSPICIOUS,result.assessment)
        assertTrue(result.evidence.all { it.source.startsWith("Decoded QR") })
        assertTrue(result.completed.any { it.contains("No OCR text") })
        store.save("","Document",result=result,qrPayload=qr,extractionSource=draft.extractionSource)
        val saved=store.read().single()
        assertEquals(qr,saved.qrPayload); assertEquals("PDF page 1",saved.extractionSource)
        assertEquals(result,saved.result)
        assertEquals(qr,org.json.JSONObject(ResultCodec.export(saved)).getString("qrPayload"))
    }
}
