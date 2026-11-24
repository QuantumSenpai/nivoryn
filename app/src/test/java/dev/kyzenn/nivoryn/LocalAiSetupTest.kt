package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.setup.SupportedModelDownload
import org.junit.Assert.*
import org.junit.Test

class LocalAiSetupTest {
    @Test fun setupRequiresRealInferenceAndNeverAcceptsFallbackAsReady() {
        assertFalse(SupportedModelDownload.ready("Curated fallback · Trusted Llama resources missing"))
        assertFalse(SupportedModelDownload.ready("Verified resources downloaded"))
        assertTrue(SupportedModelDownload.ready("Llama generated explanation · validated references"))
    }
    @Test fun storagePreflightAllowsRoomForDownloadAndPrivateStaging() {
        SupportedModelDownload.requireSpace(2_600_000_000,1_300_000_000,1_138_135_470)
        assertThrows(IllegalArgumentException::class.java) { SupportedModelDownload.requireSpace(1_000_000_000,3_000_000_000,1_138_135_470) }
        assertThrows(IllegalArgumentException::class.java) { SupportedModelDownload.requireSpace(3_000_000_000,100_000_000,1_138_135_470) }
    }
    @Test fun onlyBuildPinnedPublisherResourcesHaveDownloadUrls() {
        assertTrue(SupportedModelDownload.url("model").endsWith("Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte"))
        assertTrue(SupportedModelDownload.url("tokenizer").endsWith("tokenizer.model"))
        assertTrue(SupportedModelDownload.url("model").contains("/911221e0f49d3354cbd4697a9c09feee25cadec7/"))
        assertThrows(IllegalArgumentException::class.java) { SupportedModelDownload.url("https://example.org/arbitrary-model") }
    }
}
