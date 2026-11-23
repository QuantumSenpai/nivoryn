package dev.kyzenn.nivoryn

import android.content.ContentValues
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.integration.LocalExtraction
import dev.kyzenn.nivoryn.documents.DocumentInspector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.BeforeClass
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import android.os.SystemClock

/** Actual content-provider/decoder paths, using only generated benign files. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class ImportedExtractionDeviceTest {
    @get:Rule val testName = TestName()
    companion object {
        @JvmStatic @BeforeClass fun resetMetrics() {
            File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "extraction-metrics.json").writeText("[]")
        }
    }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private suspend fun extract(draft: CheckDraft, hindi: Boolean): dev.kyzenn.nivoryn.integration.ExtractionResult {
        val start = SystemClock.elapsedRealtime()
        val row = JSONObject().put("test", testName.methodName).put("hindi", hindi)
        try {
            return LocalExtraction.readDetailed(context, draft, hindi).also {
                row.put("success", true).put("textChars", it.text.length).put("qrChars", it.qr.length).put("source", it.source)
            }
        } catch (e: Exception) { row.put("success", false).put("exception", e.javaClass.simpleName); throw e }
        finally {
            row.put("elapsedMs", SystemClock.elapsedRealtime() - start)
            val file = File(context.filesDir, "extraction-metrics.json")
            file.writeText(JSONArray(file.readText()).put(row).toString(2))
        }
    }
    private suspend fun imported(mime: String, data: ByteArray, body: suspend (Uri) -> Unit) {
        val collection = if (mime == "application/pdf") MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "nivoryn-test-${java.util.UUID.randomUUID()}" + if (mime == "application/pdf") ".pdf" else if (mime == "image/jpeg") ".jpg" else ".png")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(collection, values))
        try {
            resolver.openOutputStream(uri, "w")!!.use { it.write(data) }
            // These are app-owned pending fixtures, never other users' media.
            body(uri)
        } finally { assertEquals("Owned fixture must be deleted", 1, resolver.delete(uri, null, null)) }
    }
    private fun bitmap(text: String) = Bitmap.createBitmap(1000, 250, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it); canvas.drawColor(Color.WHITE)
        canvas.drawText(text, 28f, 140f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 58f })
    }
    private fun encoded(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG) = ByteArrayOutputStream().use {
        check(bitmap.compress(format, 95, it)); it.toByteArray()
    }
    private fun qr(payload: String): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 400, 400)
        return Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).also { image ->
            for (y in 0 until 400) for (x in 0 until 400) image.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    @Test fun jpegOrientationIsAppliedBeforeRealOcr() = runBlocking {
        val original = bitmap("Share your OTP now")
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { setRotate(90f) }, false)
        try {
            imported("image/jpeg", encoded(rotated, Bitmap.CompressFormat.JPEG)) { uri ->
                context.contentResolver.openFileDescriptor(uri, "rw")!!.use {
                    ExifInterface(it.fileDescriptor).apply {
                        setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_270.toString()); saveAttributes()
                    }
                }
                val result = extract(CheckDraft(kind = "Screenshot", attachmentUri = uri.toString(), mime = "image/jpeg"), false)
                assertTrue(result.text, result.text.contains("OTP", true))
                assertEquals("Imported image (orientation corrected)", result.source)
            }
        } finally { original.recycle(); rotated.recycle() }
    }
    @Test fun bundledDevanagariWorksWithoutDownloadingAnEngine() = runBlocking {
        val image = bitmap("अपना ओटीपी बताएं")
        try {
            imported("image/png", encoded(image)) { uri ->
                val result = extract(CheckDraft(kind = "Screenshot", attachmentUri = uri.toString(), mime = "image/png"), true)
                assertTrue("No Devanagari extracted: ${result.text}", result.text.any { it in '\u0900'..'\u097f' })
            }
        } finally { image.recycle() }
    }
    @Test fun qrImportDecodesTheActualPayloadWithoutOpeningIt() = runBlocking {
        val payload = "https://paypal.com@evil.example/verify"
        val image = qr(payload)
        try {
            imported("image/png", encoded(image)) { uri ->
                val result = extract(CheckDraft(kind = "QR", attachmentUri = uri.toString(), mime = "image/png"), false)
                assertEquals(payload, result.qr); assertEquals(payload, result.text)
            }
        } finally { image.recycle() }
    }
    private fun pdf(pages: Int, withText: Boolean = true, payload: String = "PNR: XYZ999"): ByteArray {
        val document = PdfDocument()
        try {
        repeat(pages) { index ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(700, 900, index + 1).create())
            page.canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 40f }
            if (withText) {
                page.canvas.drawText("Share your OTP now", 40f, 90f, paint)
                page.canvas.drawText("PNR: ABC123", 40f, 160f, paint)
            }
            val code = qr(payload)
            try { page.canvas.drawBitmap(code, null, Rect(40, 240, 440, 640), null) } finally { code.recycle() }
            document.finishPage(page)
        }
        return ByteArrayOutputStream().use { document.writeTo(it); it.toByteArray() }
        } finally { document.close() }
    }
    @Test fun pdfOcrAndQrRemainAssociatedWithPageOne() = runBlocking {
        imported("application/pdf", pdf(2)) { uri ->
            val result = extract(CheckDraft(kind = "Document", attachmentUri = uri.toString(), mime = "application/pdf"), false)
            assertEquals("PDF page 1", result.source)
            assertTrue(result.text, result.text.contains("OTP", true))
            assertEquals("PNR: XYZ999", result.qr)
            assertTrue(DocumentInspector.inspect(result.text, result.qr).any { it.title == "Document / QR field mismatch" })
        }
    }
    @Test fun corruptAndExcessivePagePdfsAreRejected() = runBlocking {
        for (data in listOf("%PDF-invalid-data".toByteArray(), pdf(21))) {
            imported("application/pdf", data) { uri ->
                val attempt = runCatching { extract(CheckDraft(kind = "Document", attachmentUri = uri.toString(), mime = "application/pdf"), false) }
                assertTrue("Unsupported PDF must fail instead of producing content", attempt.isFailure)
                assertTrue("Preview must enforce the same PDF policy",runCatching {
                    LocalExtraction.preview(context,CheckDraft(kind="Document",attachmentUri=uri.toString(),mime="application/pdf")).recycle()
                }.isFailure)
            }
        }
    }
    @Test fun unreadableImageDoesNotInventExtractedContent() = runBlocking {
        val image = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        try {
            imported("image/png", encoded(image)) { uri ->
                val result = extract(CheckDraft(kind = "Screenshot", attachmentUri = uri.toString(), mime = "image/png"), false)
                assertTrue(result.text.isBlank()); assertTrue(result.qr.isBlank())
            }
        } finally { image.recycle() }
    }
    @Test fun qrOnlyPdfIsAnalysedWithoutClaimingOcrContent() = runBlocking {
        val payload="https://paypal.com@evil.example/verify"
        imported("application/pdf",pdf(1,withText=false,payload=payload)) { uri ->
            val extraction=extract(CheckDraft(kind="Document",attachmentUri=uri.toString(),mime="application/pdf"),false)
            assertTrue(extraction.text,extraction.text.isBlank())
            assertEquals(payload,extraction.qr)
            val result=dev.kyzenn.nivoryn.domain.AnalyseInput().analyse(CheckDraft(kind="Document",qrPayload=extraction.qr,extractionSource=extraction.source))
            assertEquals(dev.kyzenn.nivoryn.domain.Assessment.SUSPICIOUS,result.assessment)
            assertTrue(result.completed.any { it.contains("No OCR text") })
            assertTrue(result.evidence.all { it.source.startsWith("Decoded QR") })
        }
    }
}
