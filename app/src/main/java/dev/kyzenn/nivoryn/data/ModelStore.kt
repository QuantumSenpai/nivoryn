package dev.kyzenn.nivoryn.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

data class InstalledModel(val model: File, val tokenizer: File, val version: String)
data class ModelProfile(val version: String, val modelHash: String, val tokenHash: String, val modelBytes: Long, val tokenBytes: Long) {
    val id get() = modelHash.take(32) + tokenHash.take(32)
    fun hash(name: String) = if (name == "model") modelHash else tokenHash
    fun size(name: String) = if (name == "model") modelBytes else tokenBytes
}

/** Catalogue is a build-pinned trust root, never taken from an imported resource sidecar. */
class ModelStore(context: Context, private val catalogue: () -> String = {
    context.assets.open("trusted-model.json").bufferedReader().use { it.readText() }
}) {
    val directory = File(context.noBackupFilesDir, "llama")
    private val active = AtomicFile(File(directory, "active.json"))
    fun profiles(): List<ModelProfile> {
        val text = catalogue(); require(text.length <= 16384)
        val root = JSONObject(text)
        val rows = root.optJSONArray("models")
        val count = rows?.length() ?: 1; require(count in 1..4)
        return List(count) { i ->
            val j = rows?.getJSONObject(i) ?: root
            require(j.getString("family") == "Llama" && j.getString("runtime") == "executorch-1.2.0")
            val model = j.getString("modelSha256"); val token = j.getString("tokenizerSha256")
            require(listOf(model, token).all { it.matches(Regex("[a-f0-9]{64}")) })
            ModelProfile(j.getString("version"), model, token, j.getLong("modelBytes"), j.getLong("tokenizerBytes")).also {
                require(it.version.length in 1..100 && it.modelBytes in 1..2_000_000_000 && it.tokenBytes in 1..32_000_000)
            }
        }.also { require(it.map(ModelProfile::id).distinct().size == it.size) }
    }
    fun preferred() = profiles().last()
    fun trustedDigest(name: String): String { requireName(name); return preferred().hash(name) }
    fun stage(profile: ModelProfile) = File(directory, "staging/${profile.id}")
    private fun version(profile: ModelProfile) = File(directory, "versions/${profile.id}")
    private fun state(): JSONObject = if (active.baseFile.exists() || File(active.baseFile.path + ".bak").exists()) {
        require(active.baseFile.length() <= 4096); JSONObject(String(active.readFully(), Charsets.UTF_8))
    } else JSONObject()
    fun installed(): InstalledModel? = runCatching {
        val profile = profiles().find { it.id == state().optString("active") } ?: return null
        verified(profile, version(profile))?.let { InstalledModel(it.first, it.second, profile.version) }
    }.getOrNull()
    private fun verified(profile: ModelProfile, folder: File, cancelled: () -> Boolean = { false }): Pair<File, File>? {
        val model = File(folder, "model.pte"); val token = File(folder, "tokenizer.bin")
        return if (model.length() == profile.modelBytes && token.length() == profile.tokenBytes && matches(model, profile.modelHash, cancelled) && matches(token, profile.tokenHash, cancelled)) model to token else null
    }
    fun activateIfReady(profile: ModelProfile, cancelled: () -> Boolean = { false }): Boolean {
        checkCancellation(cancelled)
        val staged = stage(profile)
        val target = version(profile)
        if (verified(profile, target, cancelled) == null) {
            if (verified(profile, staged, cancelled) == null) return false
            target.parentFile!!.mkdirs()
            if (target.exists()) error("Existing version is corrupt; delete resources before recovery")
            checkCancellation(cancelled)
            check(staged.renameTo(target)) { "Could not activate complete resource pair" }
        }
        check(verified(profile, target, cancelled) != null)
        checkCancellation(cancelled)
        if (staged.exists()) staged.deleteRecursively()
        val old = state().optString("active")
        if (old != profile.id) writeState(profile.id, old, cancelled)
        return true
    }
    fun rollback(cancelled: () -> Boolean = { false }) {
        checkCancellation(cancelled)
        val state = state(); val previous = state.optString("previous")
        val profile = profiles().find { it.id == previous } ?: error("No trusted previous version")
        check(verified(profile, version(profile), cancelled) != null) { "Previous resources are missing or corrupt" }
        writeState(previous, state.getString("active"), cancelled)
    }
    fun deleteAll() { check(!directory.exists() || directory.deleteRecursively()) { "Could not delete every resource" } }
    private fun writeState(id: String, previous: String, cancelled: () -> Boolean = { false }) {
        directory.mkdirs()
        val stream = active.startWrite()
        try {
            stream.write(JSONObject().put("active", id).put("previous", previous).toString().toByteArray())
            checkCancellation(cancelled)
            active.finishWrite(stream)
        }
        catch (e: Exception) { active.failWrite(stream); throw e }
    }
    companion object {
        fun requireName(name: String) = require(name in listOf("model", "tokenizer"))
        fun checkCancellation(cancelled: () -> Boolean) {
            if (cancelled() || Thread.currentThread().isInterrupted) throw java.util.concurrent.CancellationException()
        }
        fun matches(file: File, expected: String, cancelled: () -> Boolean = { false }) = expected.matches(Regex("[a-f0-9]{64}")) && file.isFile && digest(file, cancelled) == expected
        fun digest(file: File, cancelled: () -> Boolean = { false }): String {
            val hash = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) {
                checkCancellation(cancelled)
                val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n)
            } }
            checkCancellation(cancelled)
            return hash.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
