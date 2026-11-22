package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.data.FrontendDemoRepository
import dev.kyzenn.nivoryn.domain.*
import org.junit.Assert.*
import org.junit.Test

class DemoContractTest {
    private val repo = FrontendDemoRepository()
    @Test fun customInputNeverGetsASampleVerdict() {
        assertNull(repo.resultFor(CheckDraft(text = "KYC urgent blocked account")))
    }
    @Test fun editedSampleLosesItsVerdict() {
        val sample = repo.samples.first()
        assertNotNull(repo.resultFor(CheckDraft(sample.text, sample.id)))
        assertNull(repo.resultFor(CheckDraft(sample.text + " edited", sample.id)))
    }
    @Test fun everySampleHasHonestCoverageAndEvidence() {
        assertEquals(8, repo.samples.size)
        repo.samples.forEach { sample ->
            val result = repo.resultFor(CheckDraft(sample.text, sample.id))!!
            assertTrue(result.unavailable.isNotEmpty())
            assertTrue(result.unverified.isNotEmpty())
            assertTrue(result.evidence.all { sample.text.contains(it.quote) })
        }
    }
    @Test fun draftValidationRejectsEmptyAndPreservesCustomText() {
        assertFalse(CheckDraft().canReview)
        assertTrue(CheckDraft("my message").canReview)
        assertTrue(CheckDraft(attachmentUri = "content://sample").canReview)
    }
    @Test fun caseItemsAreUniqueAndOrdered() {
        val case = DemoCase("1", "Work offer").add("task").add("kyc").add("task")
        assertEquals(listOf("task", "kyc"), case.sampleIds)
        assertEquals(listOf("kyc"), case.remove("task").sampleIds)
    }
}
