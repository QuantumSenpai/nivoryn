package dev.kyzenn.nivoryn.integration

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import dev.kyzenn.nivoryn.domain.CheckDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocalExtraction {
    private const val MAX_BYTES = 20 * 1024 * 1024
    suspend fun read(context: Context, draft: CheckDraft, hindi: Boolean): String {
        require(draft.kind in listOf("Screenshot", "Document", "QR"))
        val uri = Uri.parse(draft.attachmentUri)
        require(uri.scheme == "content" && draft.bytes <= MAX_BYTES)
        val bitmap = withContext(Dispatchers.IO) {
            // Bound actual bytes, not just the provider's untrusted size metadata.
            val bytes = context.contentResolver.openInputStream(uri)!!.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break
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
                            require(pdf.pageCount > 0)
                            pdf.openPage(0).use { page ->
                                val scale = minOf(1600f/page.width, 1800f/page.height)
                                Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1), (page.height*scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
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
                requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
            }
        }
        if (draft.kind == "QR") return withContext(Dispatchers.Default) {
            try { val pixels = IntArray(bitmap.width*bitmap.height); bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                QrDecoder.decode(pixels, bitmap.width, bitmap.height)
            } finally { bitmap.recycle() }
        }
        val client = try {
            if (hindi) TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()) else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) { bitmap.recycle(); throw e }
        val task = try { client.process(InputImage.fromBitmap(bitmap, 0)) }
            catch (e: Exception) { client.close(); bitmap.recycle(); throw e }
        return suspendCancellableCoroutine { continuation ->
            task
                .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result.text.take(12000)) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
                .addOnCompleteListener { client.close(); bitmap.recycle() }
        }
    }
}
