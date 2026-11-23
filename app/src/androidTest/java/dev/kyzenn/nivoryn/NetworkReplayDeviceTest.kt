package dev.kyzenn.nivoryn

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dev.kyzenn.nivoryn.integration.LocalInspection
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Safe synthetic packets are read from the actual Android provider, never transmitted. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class NetworkReplayDeviceTest {
    @Test fun importsBenignScanAndCorruptCapturesWithoutClaimingLiveMonitoring() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val reports = JSONArray()
        for (count in listOf(4, 40, -1)) {
            val data = if (count < 0) "invalid PCAP".toByteArray() else capture(count)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "nivoryn-safe-${java.util.UUID.randomUUID()}.pcap")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            try {
                resolver.openOutputStream(uri, "w")!!.use { it.write(data) }
                val attempt = runCatching { LocalInspection.inspect(context, uri, network = true) }
                if (count < 0) {
                    assertTrue(attempt.isFailure)
                    reports.put(JSONObject().put("fixture", "corrupt").put("rejected", true))
                } else {
                    val report = attempt.getOrThrow()
                    assertTrue(report, report.startsWith("Replay only · $count records, $count supported"))
                    assertTrue(report, report.contains("Observed flows (first 64 of $count):"))
                    assertEquals(count == 40, report.contains("possible scanning"))
                    assertFalse(report.contains("confirmed intrusion"))
                    reports.put(JSONObject().put("fixture", if (count == 4) "benign" else "port-sweep").put("report", report))
                }
            } finally { assertEquals(1, resolver.delete(uri, null, null)) }
        }
        File(context.filesDir, "network-replay-smoke.json").writeText(reports.toString(2))
    }

    private fun capture(count: Int): ByteArray {
        val bytes = ByteBuffer.allocate(24 + count * 56).order(ByteOrder.LITTLE_ENDIAN)
        bytes.putInt(0xa1b2c3d4.toInt()).putShort(2).putShort(4).putInt(0).putInt(0).putInt(65535).putInt(101)
        repeat(count) { index ->
            bytes.putInt(1).putInt(index).putInt(40).putInt(40)
            val ip = ByteArray(40)
            ip[0] = 0x45; ip[3] = 40; ip[9] = 6
            ip[12] = 10; ip[15] = 1; ip[16] = 10; ip[19] = 2
            ip[20] = 0x30; ip[21] = 0x39; ip[23] = (index + 1).toByte()
            ip[32] = 0x50; ip[33] = 2
            bytes.put(ip)
        }
        return bytes.array()
    }
}
