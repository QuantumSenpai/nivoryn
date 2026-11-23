package dev.kyzenn.nivoryn.integration

import android.content.Context
import android.net.Uri
import dev.kyzenn.nivoryn.malware.ApkInspector
import dev.kyzenn.nivoryn.network.PcapInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File

/** User-selected content only. No fetching, installation or execution. */
object LocalInspection {
    suspend fun inspect(context: Context, uri: Uri, network: Boolean): String {
        require(uri.scheme == "content") { "Select an input through the document picker" }
        return withTimeout(30_000) {
            withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(65536)
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        require(out.size().toLong() + n <= 20_000_000) { "Input exceeds 20 MB" }
                        out.write(buffer, 0, n)
                    }
                    out.toByteArray()
                } ?: error("Cannot read selected input")
                ensureActive()
                val result = if (network) {
                    val report = PcapInspector.inspect(bytes)
                    "Replay only · ${report.packets} records, ${report.supported} supported IPv4 TCP/UDP packets, ${report.bytes} original bytes\n\n" +
                        report.findings.joinToString("\n\n") +
                        "\n\nObserved flows (first 64 of ${report.flows.size}):\n" +
                        report.flows.take(64).joinToString("\n") {
                            "${it.key.source}:${it.key.sourcePort} → ${it.key.destination}:${it.key.destinationPort} ${it.key.protocol} · ${it.packets} packets · ${it.ipBytes} IP bytes · ${it.synWithoutAck} SYN without ACK"
                        }
                } else {
                    val file = File.createTempFile("inspect-", ".apk", context.cacheDir)
                    try { file.writeBytes(bytes); ApkInspector.inspect(context, file) }
                    finally { file.delete() }
                }
                ensureActive()
                result.take(20000)
            }
        }
    }
}
