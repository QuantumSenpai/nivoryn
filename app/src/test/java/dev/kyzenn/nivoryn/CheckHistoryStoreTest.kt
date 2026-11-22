package dev.kyzenn.nivoryn

import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.data.CheckHistoryStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class CheckHistoryStoreTest {
    @Test fun historyIsExplicitBoundedDeduplicatedAndExpires() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = CheckHistoryStore(ApplicationProvider.getApplicationContext()) { key }
        val now = System.currentTimeMillis()
        assertTrue(store.read(now).isEmpty())
        repeat(55) { store.save("Private text $it", "Text", now) }
        assertEquals(50, store.read(now).size)
        store.save("Private text 54", "Text", now)
        assertEquals(50, store.read(now).size)
        assertEquals("Private text 54", store.read(now).first().text)
        assertTrue(store.read(now + 31L*24*60*60*1000).isEmpty())
        store.save("New private text", "Text", now)
        store.clear(); assertTrue(store.read(now).isEmpty())
    }
    @Test fun failedAuthenticationDoesNotOverwriteTheExistingStore() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = CheckHistoryStore(context) { key }
        store.save("Original private text", "Text")
        val file = java.io.File(context.noBackupFilesDir, "checks-v1.enc")
        val bytes = file.readBytes()
        val wrong = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertThrows(Exception::class.java) { CheckHistoryStore(context) { wrong }.save("Overwrite attempt", "Text") }
        assertArrayEquals(bytes, file.readBytes())
        assertEquals("Original private text", store.read().single().text)
        assertFalse(file.readText().contains("Original private text"))
    }
}
