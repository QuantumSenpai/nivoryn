package dev.kyzenn.nivoryn.integration

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import dev.kyzenn.nivoryn.domain.CheckDraft
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ExtractionResult(val text: String, val qr: String, val source: String)

object LocalExtraction {
    private const val MAX_BYTES = 20 * 1024 * 1024
    suspend fun read(context: Context, draft: CheckDraft, hindi: Boolean): String = readDetailed(context, draft, hindi).text
    suspend fun readDetailed(context: Context, draft: CheckDraft, hindi: Boolean): ExtractionResult =
        kotlinx.coroutines.withTimeout(30_000) { readBounded(context, draft, hindi) }
    suspend fun preview(context: Context, draft: CheckDraft): Bitmap = kotlinx.coroutines.withTimeout(30_000) {
        loadBitmap(context, draft)
    }
    private suspend fun loadBitmap(context: Context, draft: CheckDraft): Bitmap {
        require(draft.kind in listOf("Screenshot", "Document", "QR"))
        val uri = draft.attachmentUri.toUri()
        require(uri.scheme == "content" && draft.bytes <= MAX_BYTES)
        return withContext(Dispatchers.IO) {
            // Bound actual bytes, not just the provider's untrusted size metadata.
            val bytes = context.contentResolver.openInputStream(uri)!!.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) { kotlin.coroutines.coroutineContext.ensureActive(); val count = input.read(buffer); if (count < 0) break
                    require(output.size() + count <= MAX_BYTES); output.write(buffer, 0, count) }
                output.toByteArray()
            }
            if (draft.mime == "application/pdf") {
                require(bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-")
                val temp = java.io.File.createTempFile("review-", ".pdf", context.cacheDir)
                try {
                    temp.writeBytes(bytes)
                    android.os.ParcelFileDescriptor.open(temp, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        PdfRenderer(fd).use { pdf ->
                            require(pdf.pageCount in 1..20) { "PDF exceeds the 20-page input limit" }
                            pdf.openPage(0).use { page ->
                                val scale = minOf(1600f/page.width, 1800f/page.height)
                                createBitmap((page.width*scale).toInt().coerceAtLeast(1), (page.height*scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
                                    it.eraseColor(Color.WHITE); page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                }
                            }
                        }
                    }
                } finally { temp.delete() }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000 && bounds.outWidth.toLong()*bounds.outHeight <= 100_000_000)
                val options = BitmapFactory.Options().apply { inSampleSize = generateSequence(1) { it*2 }.first { bounds.outWidth/it <= 1600 && bounds.outHeight/it <= 1600 } }
                val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
                val orientation = runCatching {
                    androidx.exifinterface.media.ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)
                }.getOrDefault(1)
                val matrix = Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f)
                        3 -> setRotate(180f)
                        4 -> setScale(1f, -1f)
                        5 -> { setRotate(90f); postScale(-1f, 1f) }
                        6 -> setRotate(90f)
                        7 -> { setRotate(270f); postScale(-1f, 1f) }
                        8 -> setRotate(270f)
                    }
                }
                if (orientation in 2..8) Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                    if (it !== decoded) decoded.recycle()
                } else decoded
            }
        }
    }
    private suspend fun readBounded(context: Context, draft: CheckDraft, hindi: Boolean): ExtractionResult {
        val bitmap = loadBitmap(context, draft)
        var taskOwnsBitmap = false
        try {
            val source = if (draft.mime == "application/pdf") "PDF page 1" else "Imported image (orientation corrected)"
            if (draft.kind == "QR") return withContext(Dispatchers.Default) {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val payload = QrDecoder.decode(pixels, bitmap.width, bitmap.height)
                ExtractionResult(payload, payload, source)
            }
            val qr = withContext(Dispatchers.Default) {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                try { QrDecoder.decode(pixels, bitmap.width, bitmap.height) }
                catch (_: com.google.zxing.NotFoundException) { "" }
                catch (_: com.google.zxing.FormatException) { "" }
                catch (_: com.google.zxing.ChecksumException) { "" }
            }
            val client = if (hindi) TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val task = try { client.process(InputImage.fromBitmap(bitmap, 0)) }
                catch (e: Exception) { client.close(); throw e }
            // ML Kit owns the pixels until task completion, even after coroutine cancellation.
            taskOwnsBitmap = true
            task.addOnCompleteListener { client.close(); bitmap.recycle() }
            return suspendCancellableCoroutine { continuation ->
                task
                    .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(ExtractionResult(result.text.take(12000), qr, source)) }
                    .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
            }
        } finally { if (!taskOwnsBitmap) bitmap.recycle() }
    }
}
