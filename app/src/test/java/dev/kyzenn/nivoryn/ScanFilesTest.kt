package dev.kyzenn.nivoryn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.integration.ScanFiles
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ScanFilesTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @org.junit.Before fun clearProviderTestCache() {
        org.robolectric.shadows.ShadowStatFs.registerStats(File(context.cacheDir,"scans").path,1000000,900000,900000)
        org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(androidx.core.content.FileProvider::class.java,"sCache").clear()
    }
    @Test fun captureUsesNarrowContentUriAndClearRemovesOnlyItsOwnFile() {
        val uri=ScanFiles.create(context)
        assertEquals("content",uri.scheme)
        assertEquals(context.packageName+".scan-files",uri.authority)
        val other=File(context.cacheDir,"unrelated.txt").apply { writeText("keep") }
        assertEquals(1,File(context.cacheDir,"scans").listFiles().orEmpty().size)
        ScanFiles.release(context,uri.toString())
        assertTrue(File(context.cacheDir,"scans").listFiles().orEmpty().isEmpty())
        assertEquals("keep",other.readText())
    }
    @Test fun lowStorageRefusesCaptureWithoutAllocatingAnImage() {
        org.robolectric.shadows.ShadowStatFs.registerStats(File(context.cacheDir,"scans").path,1000,1,1)
        assertThrows(IllegalArgumentException::class.java) { ScanFiles.create(context) }
        assertTrue(File(context.cacheDir,"scans").listFiles().orEmpty().isEmpty())
    }
    @Test fun abandonedCapturesAreBoundedAndRecentDraftsAreNotEvicted() {
        repeat(8) { ScanFiles.create(context) }
        assertThrows(IllegalArgumentException::class.java) { ScanFiles.create(context) }
        assertEquals(8,File(context.cacheDir,"scans").listFiles().orEmpty().size)
    }
}
