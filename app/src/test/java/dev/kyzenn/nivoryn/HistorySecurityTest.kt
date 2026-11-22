package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.data.*
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class HistorySecurityTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    @Test fun recordsRoundTripWithFreshNoncesAndNoPlaintext() {
        val cipher = HistoryCipher(key())
        val content = "Private message · नमस्ते".toByteArray()
        val one = cipher.encrypt(content); val two = cipher.encrypt(content)
        assertArrayEquals(content, cipher.decrypt(one)); assertFalse(one.contentEquals(two))
        assertFalse(one.toString(Charsets.UTF_8).contains("Private message"))
    }
    @Test fun modificationsWrongKeysAndTruncationAreRejected() {
        val cipher = HistoryCipher(key()); val bytes = cipher.encrypt("Sensitive data".toByteArray())
        for (index in listOf(0, 1, 12, bytes.lastIndex)) {
            val modified = bytes.clone(); modified[index] = (modified[index].toInt() xor 1).toByte()
            assertThrows(Exception::class.java) { cipher.decrypt(modified) }
        }
        assertThrows(Exception::class.java) { HistoryCipher(key()).decrypt(bytes) }
        assertThrows(Exception::class.java) { cipher.decrypt(bytes.copyOf(15)) }
    }
}
