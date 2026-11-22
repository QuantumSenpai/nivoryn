package dev.kyzenn.nivoryn.data

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Version is authenticated too; corrupted/modified records are never accepted. */
class HistoryCipher(private val key: SecretKey) {
    fun encrypt(plain: ByteArray): ByteArray {
        require(plain.size <= 1024 * 1024)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(byteArrayOf(1))
        return byteArrayOf(1) + cipher.iv + cipher.doFinal(plain)
    }
    fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size in 30..(1024 * 1024 + 29) && bytes[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(byteArrayOf(1))
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size))
    }
}
