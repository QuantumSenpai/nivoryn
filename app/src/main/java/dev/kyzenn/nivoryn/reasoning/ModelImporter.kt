package dev.kyzenn.nivoryn.reasoning

import dev.kyzenn.nivoryn.data.ModelStore
import java.io.InputStream
import java.io.File
import java.io.RandomAccessFile

class ModelImporter(private val store: ModelStore) {
    /** Interrupted copy stays private. Re-selection checks the prefix, then final pinned digest. */
    fun import(name: String, input: InputStream, cancelled: () -> Boolean = { false }) {
        ModelStore.requireName(name)
        input.use { source ->
            val profile = store.preferred(); val expected = profile.hash(name); val max = profile.size(name)
            val staging = store.stage(profile); staging.mkdirs()
            val part = File(staging, "$name.part")
            require(staging.usableSpace > (max - part.length()).coerceAtLeast(0) + 64_000_000L) { "Insufficient private storage" }
            RandomAccessFile(part, "rw").use { out ->
                val buffer = ByteArray(65536); val previous = ByteArray(65536); var position = 0L
                while (true) {
                    ModelStore.checkCancellation(cancelled)
                    val n = source.read(buffer); if (n < 0) break
                    require(position + n <= max) { "Resource exceeds trusted size" }
                    val present = if (position + n <= out.length()) { out.seek(position); out.read(previous, 0, n) == n && (0 until n).all { previous[it] == buffer[it] } } else false
                    if (!present) { out.seek(position); out.write(buffer, 0, n) }
                    position += n
                }
                ModelStore.checkCancellation(cancelled)
                require(position == max) { "Incomplete resource; reselect to resume" }
                out.setLength(position); out.fd.sync()
            }
            if (!ModelStore.matches(part, expected, cancelled)) { part.delete(); error("Resource does not match trusted release") }
            ModelStore.checkCancellation(cancelled)
            val ready = File(staging, if (name == "model") "model.pte" else "tokenizer.bin")
            check(part.renameTo(ready)) { "Could not stage resource" }
            store.activateIfReady(profile, cancelled)
        }
    }
}
