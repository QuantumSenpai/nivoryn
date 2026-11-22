package dev.kyzenn.nivoryn

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dev.kyzenn.nivoryn.integration.QrDecoder
import org.junit.Assert.*
import org.junit.Test

class QrDecoderTest {
    @Test fun decodesActualQrPixelsWithoutOpeningPayload() {
        val payload = "upi://pay?pa=example%40upi&am=999&cu=INR"
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320)
        val pixels = IntArray(320*320) { if (matrix[it%320,it/320]) 0xff000000.toInt() else 0xffffffff.toInt() }
        assertEquals(payload, QrDecoder.decode(pixels, 320, 320))
    }
    @Test fun invalidDimensionsAndNonQrImagesFail() {
        assertThrows(Exception::class.java) { QrDecoder.decode(IntArray(10), 300, 300) }
        assertThrows(Exception::class.java) { QrDecoder.decode(IntArray(300*300) { -1 }, 300, 300) }
    }
}
