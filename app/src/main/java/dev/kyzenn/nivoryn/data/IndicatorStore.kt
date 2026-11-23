package dev.kyzenn.nivoryn.data

import android.content.Context
import android.util.AtomicFile
import dev.kyzenn.nivoryn.detection.IndicatorPack
import org.json.JSONObject
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Detached signature over exact payload bytes. Keys are pinned in the signed application. */
class IndicatorStore(context: Context, private val keys: () -> String = {
    context.assets.open("trusted-rule-keys.json").bufferedReader().use { it.readText() }
}) {
    private val folder = File(context.noBackupFilesDir, "indicators")
    private val active = AtomicFile(File(folder, "active.json"))
    private val previous = AtomicFile(File(folder, "previous.json"))
    fun verify(envelope: String): IndicatorPack {
        require(envelope.length <= 1_500_000)
        val j = JSONObject(envelope); val root = keys(); require(root.length <= 16384)
        val pinned = JSONObject(root); require(pinned.length() in 1..4)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(pinned.getString(j.getString("keyId")))))
        require((key as java.security.interfaces.RSAPublicKey).modulus.bitLength() >= 2048)
        val payload = Base64.getDecoder().decode(j.getString("payload")); require(payload.size <= 1_000_000)
        val signature = Base64.getDecoder().decode(j.getString("signature")); require(signature.size <= 512)
        val verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(key); verifier.update(payload)
        require(verifier.verify(signature)) { "Indicator signature is not trusted" }
        return IndicatorPack.parse(String(payload, Charsets.UTF_8))
    }
    private fun read(file: AtomicFile): String {
        require(file.baseFile.length() <= 1_500_000)
        return String(file.readFully(), Charsets.UTF_8)
    }
    fun load(): IndicatorPack? = synchronized(lock) { runCatching { verify(read(active)) }.getOrNull() }
    fun install(envelope: String, now: Long = System.currentTimeMillis()) = synchronized(lock) {
        val pack = verify(envelope); require(pack.fresh(now)) { "Indicator pack is expired or future-dated" }
        val current = load(); require(current == null || pack.version > current.version) { "Refusing silent rule downgrade" }
        if (current != null) write(previous, read(active))
        write(active, envelope)
    }
    fun rollback(now: Long = System.currentTimeMillis()) = synchronized(lock) {
        val bytes = read(previous); val pack = verify(bytes); require(pack.fresh(now))
        val current = runCatching { read(active) }.getOrNull()
        write(active, bytes); if (current != null) write(previous, current)
    }
    fun deleteAll() = synchronized(lock) { active.delete(); previous.delete() }
    private fun write(file: AtomicFile, text: String) {
        folder.mkdirs(); val stream = file.startWrite()
        try { stream.write(text.toByteArray()); file.finishWrite(stream) }
        catch(e: Exception) { file.failWrite(stream); throw e }
    }
    companion object { private val lock = Any() }
}
