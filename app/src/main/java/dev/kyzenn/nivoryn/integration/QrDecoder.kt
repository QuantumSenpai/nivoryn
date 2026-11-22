package dev.kyzenn.nivoryn.integration

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

object QrDecoder {
    fun decode(pixels: IntArray, width: Int, height: Int): String {
        require(width in 1..1800 && height in 1..1800 && pixels.size.toLong() == width.toLong() * height)
        val source = RGBLuminanceSource(width, height, pixels)
        val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
        require(result.text.length in 1..12000)
        return result.text
    }
}
