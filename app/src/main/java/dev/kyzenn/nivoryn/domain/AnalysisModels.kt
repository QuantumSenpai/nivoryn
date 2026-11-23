package dev.kyzenn.nivoryn.domain

enum class Assessment(val label: String, val hindi: String) {
    HIGH("High risk", "उच्च जोखिम"), SUSPICIOUS("Suspicious", "संदिग्ध"),
    NONE("No clear warning found", "कोई स्पष्ट चेतावनी नहीं मिली"), UNKNOWN("Cannot assess", "आकलन संभव नहीं")
}
object AnalysisLimits {
    const val EVIDENCE = 64
    const val DESTINATIONS = 32
    const val SNAPSHOT_BYTES = 512_000
}
data class Evidence(val title: String, val quote: String, val explanation: String, val start: Int = -1, val end: Int = -1, val source: String = "Reviewed text")
enum class ResultOrigin { AUTHORED, LOCAL }
data class DemoResult(
    val assessment: Assessment, val explanation: String, val explanationHi: String,
    val evidence: List<Evidence>, val completed: List<String>, val unavailable: List<String>,
    val unverified: List<String>, val nextSteps: List<String>, val origin: ResultOrigin = ResultOrigin.AUTHORED, val explanationMode: String = "Curated local rules", val destinations: List<String> = emptyList()
)
data class DemoSample(val id: String, val title: String, val titleHi: String, val text: String, val kind: String, val result: DemoResult)
data class CheckDraft(
    val text: String = "", val sampleId: String = "", val attachmentUri: String = "",
    val filename: String = "", val mime: String = "", val bytes: Long = -1, val kind: String = "Text", val qrPayload: String = "", val extractionSource: String = ""
) { val canReview: Boolean get() = text.isNotBlank() || qrPayload.isNotBlank() || attachmentUri.isNotBlank() }
data class DemoCase(val id: String, val name: String, val sampleIds: List<String> = emptyList()) {
    fun add(sampleId: String) = copy(sampleIds = (sampleIds + sampleId).distinct())
    fun remove(sampleId: String) = copy(sampleIds = sampleIds.filterNot { it == sampleId })
}
interface DemoRepository {
    val samples: List<DemoSample>
    fun resultFor(draft: CheckDraft): DemoResult?
}
