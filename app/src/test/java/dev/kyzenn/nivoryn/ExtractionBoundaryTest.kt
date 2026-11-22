package dev.kyzenn.nivoryn

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.integration.LocalExtraction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ExtractionBoundaryTest {
    @Test fun unknownProviderSizeCannotBypassTheActualReadLimit() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val uri = Uri.parse("content://test.synthetic/oversized-image")
        var bytesRead = 0; var closed = false
        val stream = object : InputStream() {
            override fun read(): Int { bytesRead++; return 0 }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                java.util.Arrays.fill(buffer, offset, offset+length, 0.toByte()); bytesRead += length; return length
            }
            override fun close() { closed = true }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        val draft = CheckDraft(attachmentUri=uri.toString(), mime="image/png", bytes=-1, kind="Screenshot")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { LocalExtraction.read(context, draft, false) } }
        assertTrue(bytesRead > 20*1024*1024)
        assertTrue(bytesRead <= 20*1024*1024+8192)
        assertTrue("Rejected streams must be closed", closed)
    }
    @Test fun extractionRejectsNonProviderUrisAndUnsupportedKindsBeforeOpeningAnything() {
        val context: Context = ApplicationProvider.getApplicationContext()
        for (draft in listOf(CheckDraft(attachmentUri="https://example.org/image.png",kind="Screenshot"),
            CheckDraft(attachmentUri="file:///etc/hosts",kind="Screenshot"),
            CheckDraft(attachmentUri="content://test.synthetic/apk",kind="APK"))) {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { LocalExtraction.read(context, draft, false) } }
        }
    }
}
