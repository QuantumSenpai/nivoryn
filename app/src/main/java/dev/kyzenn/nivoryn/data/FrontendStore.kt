package dev.kyzenn.nivoryn.data

import android.content.Context
import androidx.core.content.edit
import dev.kyzenn.nivoryn.domain.DemoCase
import org.json.JSONArray
import org.json.JSONObject

class FrontendStore(context: Context) {
    private val prefs = context.getSharedPreferences("frontend", Context.MODE_PRIVATE)
    fun get(key: String, default: String = "") = prefs.getString(key, default) ?: default
    fun put(key: String, value: String) { prefs.edit { putString(key, value) } }
    fun cases(): List<DemoCase> = runCatching {
        val rows = JSONArray(get("cases", "[]"))
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val items = row.getJSONArray("items")
            DemoCase(row.getString("id"), row.getString("name"), List(items.length()) { items.getString(it) })
        }
    }.getOrDefault(emptyList())
    fun saveCases(cases: List<DemoCase>) = put("cases", JSONArray().also { rows ->
        cases.forEach { case -> rows.put(JSONObject().put("id", case.id).put("name", case.name).put("items", JSONArray(case.sampleIds))) }
    }.toString())
}
