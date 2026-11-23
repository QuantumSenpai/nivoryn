package dev.kyzenn.nivoryn.data

import dev.kyzenn.nivoryn.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Explicit schema, no polymorphic deserialisation. Used for encrypted snapshots and deliberate export. */
object ResultCodec {
    fun encode(result: DemoResult): JSONObject = JSONObject().apply {
        put("schema", 1); put("assessment", result.assessment.name)
        put("explanation", result.explanation); put("explanationHi", result.explanationHi)
        put("mode", result.explanationMode); put("origin", result.origin.name)
        put("evidence", JSONArray().apply { result.evidence.forEach { e -> put(JSONObject()
            .put("title", e.title).put("quote", e.quote).put("explanation", e.explanation).put("start", e.start).put("end", e.end).put("source", e.source)) } })
        put("completed", JSONArray(result.completed)); put("unavailable", JSONArray(result.unavailable))
        put("unverified", JSONArray(result.unverified)); put("actions", JSONArray(result.nextSteps))
        put("destinations", JSONArray(result.destinations))
    }
    fun decode(j: JSONObject): DemoResult {
        require(j.toString().toByteArray(Charsets.UTF_8).size <= AnalysisLimits.SNAPSHOT_BYTES && j.getInt("schema") == 1)
        fun string(key: String, max: Int = 4000) = j.getString(key).also { require(it.length <= max) }
        fun strings(key: String, maxCount: Int = 40): List<String> {
            val a = j.getJSONArray(key); require(a.length() <= maxCount)
            return List(a.length()) { a.getString(it).also { text -> require(text.length <= 2000) } }
        }
        val evidence = j.getJSONArray("evidence"); require(evidence.length() <= AnalysisLimits.EVIDENCE)
        return DemoResult(Assessment.valueOf(string("assessment", 32)), string("explanation"), string("explanationHi"),
            List(evidence.length()) { i -> val e = evidence.getJSONObject(i)
                Evidence(e.getString("title"), e.getString("quote"), e.getString("explanation"), e.getInt("start"), e.getInt("end"), e.optString("source", "Reviewed text")).also {
                    require(it.title.length <= 200 && it.quote.length <= 12000 && it.explanation.length <= 2000 && it.source.length <= 200)
                    require((it.start == -1 && it.end == -1) || (it.start in 0..12000 && it.end in it.start..12000))
                }
            }, strings("completed"), strings("unavailable"), strings("unverified"), strings("actions"),
            ResultOrigin.valueOf(string("origin", 32)), string("mode", 200), strings("destinations", AnalysisLimits.DESTINATIONS))
    }
    fun export(check: SavedCheck): String = JSONObject().put("format", "nivoryn-local-check-v1")
        .put("created", check.created).put("text", check.text).put("kind", check.kind)
        .put("qrPayload", check.qrPayload).put("extractionSource", check.extractionSource)
        .put("result", check.result?.let(::encode) ?: JSONObject.NULL).toString(2)
}
