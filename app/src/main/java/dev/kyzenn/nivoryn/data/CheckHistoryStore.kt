package dev.kyzenn.nivoryn.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.json.JSONArray
import org.json.JSONObject

data class SavedCheck(val id: String, val text: String, val kind: String, val created: Long, val result: dev.kyzenn.nivoryn.domain.DemoResult? = null,
    val qrPayload: String = "", val extractionSource: String = "") {
    val preview get() = text.ifBlank { "Decoded QR: $qrPayload" }
}

/** Explicitly saved text only; files/URIs are never copied. Call on Dispatchers.IO. */
class CheckHistoryStore(context: Context, private val keyProvider: () -> SecretKey = ::historyKey) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "checks-v1.enc"))
    private val retention = 30L * 24 * 60 * 60 * 1000
    fun read(now: Long = System.currentTimeMillis()): List<SavedCheck> = synchronized(historyLock) {
        if (!file.baseFile.exists()) return emptyList()
        require(file.baseFile.length() <= 1024 * 1024 + 29)
        val json = HistoryCipher(keyProvider()).decrypt(file.readFully()).toString(Charsets.UTF_8)
        val rows = JSONArray(json)
        require(rows.length() <= 50)
        val all = List(rows.length()) { i ->
            val row = rows.getJSONObject(i)
            SavedCheck(row.getString("id"), row.getString("text"), row.getString("kind"), row.getLong("created"), row.optJSONObject("result")?.let(ResultCodec::decode), row.optString("qr"), row.optString("source")).also {
                require(it.text.length <= 12000 && it.qrPayload.length <= 12000 && (it.text.isNotBlank() || it.qrPayload.isNotBlank()) && it.extractionSource.length <= 200 && it.id.length <= 64 && it.kind in listOf("Text", "Link", "Screenshot", "Document", "QR"))
            }
        }
        val current = all.filter { it.created <= now && now - it.created < retention }
        if (current.size != all.size) write(current)
        return current
    }
    fun save(text: String, kind: String, now: Long = System.currentTimeMillis(), result: dev.kyzenn.nivoryn.domain.DemoResult? = null,
        qrPayload: String = "", extractionSource: String = ""): List<SavedCheck> = synchronized(historyLock) {
        require(text.length <= 12000 && qrPayload.length <= 12000 && (text.isNotBlank() || qrPayload.isNotBlank()) && extractionSource.length <= 200 && kind in listOf("Text", "Link", "Screenshot", "Document", "QR"))
        result?.let { require(it.origin == dev.kyzenn.nivoryn.domain.ResultOrigin.LOCAL); ResultCodec.decode(ResultCodec.encode(it)) }
        val existing = read(now)
        fun same(check: SavedCheck) = check.text == text && check.kind == kind && check.qrPayload == qrPayload
        val id = existing.firstOrNull(::same)?.id ?: java.util.UUID.randomUUID().toString()
        val saved = listOf(SavedCheck(id, text, kind, now, result, qrPayload, extractionSource)) + existing.filterNot(::same)
        var bounded = saved.take(50)
        while (serialise(bounded).size > 1024 * 1024 && bounded.size > 1) bounded = bounded.dropLast(1)
        return bounded.also { write(it) }
    }
    fun remove(id: String): List<SavedCheck> = synchronized(historyLock) { read().filterNot { it.id == id }.also { write(it) } }
    fun clear() = synchronized(historyLock) { file.delete() }
    private fun serialise(checks: List<SavedCheck>): ByteArray {
        val rows = JSONArray()
        checks.forEach { rows.put(JSONObject().put("id", it.id).put("text", it.text).put("kind", it.kind).put("created", it.created).put("qr", it.qrPayload).put("source", it.extractionSource).put("result", it.result?.let(ResultCodec::encode) ?: JSONObject.NULL)) }
        return rows.toString().toByteArray(Charsets.UTF_8)
    }
    private fun write(checks: List<SavedCheck>) {
        val plain = serialise(checks)
        require(plain.size <= 1024 * 1024) { "History snapshot exceeds the storage budget" }
        val bytes = HistoryCipher(keyProvider()).encrypt(plain)
        val stream = file.startWrite()
        try { stream.write(bytes); file.finishWrite(stream) } catch (e: Exception) { file.failWrite(stream); throw e }
    }
}

private val historyLock = Any()

@Synchronized private fun historyKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val alias = "nivoryn-check-history-v1"
    (store.getKey(alias, null) as? SecretKey)?.let { return it }
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
        init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).build())
    }.generateKey()
}
