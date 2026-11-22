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

data class SavedCheck(val id: String, val text: String, val kind: String, val created: Long)

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
            SavedCheck(row.getString("id"), row.getString("text"), row.getString("kind"), row.getLong("created")).also {
                require(it.text.length <= 12000 && it.id.length <= 64 && it.kind in listOf("Text", "Link", "Screenshot", "Document", "QR"))
            }
        }
        val current = all.filter { it.created <= now && now - it.created < retention }
        if (current.size != all.size) write(current)
        return current
    }
    fun save(text: String, kind: String, now: Long = System.currentTimeMillis()): List<SavedCheck> = synchronized(historyLock) {
        require(text.length in 1..12000 && kind in listOf("Text", "Link", "Screenshot", "Document", "QR"))
        val saved = listOf(SavedCheck(java.util.UUID.randomUUID().toString(), text, kind, now)) + read(now).filterNot { it.text == text && it.kind == kind }
        return saved.take(50).also { write(it) }
    }
    fun remove(id: String): List<SavedCheck> = synchronized(historyLock) { read().filterNot { it.id == id }.also { write(it) } }
    fun clear() = synchronized(historyLock) { file.delete() }
    private fun write(checks: List<SavedCheck>) {
        val rows = JSONArray()
        checks.forEach { rows.put(JSONObject().put("id", it.id).put("text", it.text).put("kind", it.kind).put("created", it.created)) }
        val bytes = HistoryCipher(keyProvider()).encrypt(rows.toString().toByteArray(Charsets.UTF_8))
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
