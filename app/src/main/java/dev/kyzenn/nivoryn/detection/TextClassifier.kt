package dev.kyzenn.nivoryn.detection

import android.content.Context
import dev.kyzenn.nivoryn.domain.Evidence
import java.text.Normalizer
import java.util.Locale
import org.json.JSONObject
import kotlin.math.exp

/** Historical English SMS spam proxy. Separate from, and never a replacement for, Llama reasoning. */
class SpamClassifier private constructor(val version: String, private val prior: Double,
    private val threshold: Double, private val weights: Map<String, Double>) {
    fun supports(text: String) = text.none { it in '\u0900'..'\u097f' }
    fun probability(text: String): Double {
        require(text.length <= 12000)
        val normal = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val counts = Regex("[a-z0-9]+|[\\u0900-\\u097f]+").findAll(normal).take(600).map { it.value }.groupingBy { it }.eachCount()
        val known = counts.filterKeys { it in weights }
        if (known.size < 3) return 0.0
        val logit = prior + known.entries.sumOf { (token, count) -> count.coerceAtMost(5) * weights.getValue(token) }
        return 1.0 / (1.0 + exp(-logit.coerceIn(-40.0, 40.0)))
    }
    fun evidence(text: String): Evidence? = if (supports(text) && probability(text) >= threshold)
        Evidence("Spam-like wording (local classifier)", text.take(180), "A local statistical classifier trained on historical English SMS found spam-like wording. Spam is not proof of fraud; sender identity and multilingual accuracy remain unverified.", 0, text.take(180).length)
        else null
    companion object {
        fun load(context: Context): SpamClassifier {
            val text = context.assets.open("sms-spam-proxy-v1.json").bufferedReader().use { it.readText() }; require(text.length <= 400000)
            val j = JSONObject(text); require(j.getInt("schema") == 1 && j.getString("normalisation") == "NFKC/lower/token-v1")
            val weights = j.getJSONObject("weights"); require(weights.length() in 1..4000)
            val parsed = weights.keys().asSequence().associateWith { token ->
                require(token.length in 1..200); weights.getDouble(token).also { require(it.isFinite() && it in -50.0..50.0) }
            }
            val prior = j.getDouble("prior"); val threshold = j.getDouble("threshold")
            require(prior.isFinite() && prior in -50.0..50.0 && threshold in 0.9..0.99999)
            return SpamClassifier(j.getString("version"), prior, threshold, parsed)
        }
    }
}
