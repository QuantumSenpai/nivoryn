package dev.kyzenn.nivoryn

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.integration.LocalInspection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InspectionImportTest {
    @Test fun rejectsNonContentUrisBeforeOpeningThem() {
        val context: Context = ApplicationProvider.getApplicationContext()
        for (value in listOf("file:///etc/hosts", "https://example.org/capture.pcap")) {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { LocalInspection.inspect(context, Uri.parse(value), network = true) }
            }
        }
    }

    @Test fun unknownProviderLengthCannotBypassByteBudgetAndStreamCloses() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val uri = Uri.parse("content://test.synthetic/oversized-capture")
        var bytesRead = 0
        var closed = false
        val stream = object : InputStream() {
            override fun read(): Int = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                buffer.fill(0, offset, offset + length)
                bytesRead += length
                return length
            }
            override fun close() { closed = true }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { LocalInspection.inspect(context, uri, network = true) }
        }
        assertTrue(bytesRead in 20_000_001..20_065_536)
        assertTrue(closed)
    }
}
