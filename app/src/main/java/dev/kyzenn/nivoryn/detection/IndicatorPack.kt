package dev.kyzenn.nivoryn.detection

import java.util.Locale
import org.json.JSONObject

data class IndicatorPack(val source: String, val version: Long, val issued: Long, val expires: Long,
    val domains: Set<String>, val apkHashes: Set<String>, val certificateHashes: Set<String>) {
    fun fresh(now: Long) = now in issued..expires
    fun matchesDomain(host: String, now: Long): Boolean = fresh(now) && domains.any { host == it || host.endsWith(".$it") }
    companion object {
        fun parse(text: String): IndicatorPack {
            require(text.toByteArray().size <= 1_000_000)
            val j = JSONObject(text); require(j.getInt("schema") == 1)
            fun entries(name: String, valid: (String) -> Boolean): Set<String> {
                val a = j.getJSONArray(name); require(a.length() <= 10000)
                return List(a.length()) { a.getString(it).lowercase(Locale.ROOT).also { v -> require(valid(v)) } }.toSet()
            }
            val hex = Regex("[a-f0-9]{64}")
            val domains = entries("domains") { it.length in 3..253 && it.split('.').size >= 2 && it.split('.').all { label -> label.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) } }
            return IndicatorPack(j.getString("source"), j.getLong("version"), j.getLong("issued"), j.getLong("expires"),
                domains, entries("apkSha256", hex::matches), entries("certificateSha256", hex::matches)).also {
                require(it.source.length in 1..200 && it.version > 0 && it.issued >= 0 && it.expires > it.issued && it.expires - it.issued <= 90L * 86400000)
                require(j.getString("licence").length in 1..200)
            }
        }
    }
}
