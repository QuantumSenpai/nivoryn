package dev.kyzenn.nivoryn

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.kyzenn.nivoryn.malware.ApkInspector
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StaticApkDeviceTest {
    @Test fun execuTorchJniLoadsOnArm64() {
        assertNotNull(org.pytorch.executorch.ExecuTorchRuntime.getRuntime())
    }
    @Test fun inspectsSignedFixtureWithoutInstallingIt() {
        val context = InstrumentationRegistry.getInstrumentation().context
        // Instrumentation APK is an existing signed benign fixture, smaller than the product APK.
        val report = ApkInspector.inspect(context, File(context.applicationInfo.sourceDir))
        assertTrue(report.contains(context.packageName))
        assertTrue(report.contains("Certificate SHA-256"))
        assertFalse(report.contains("Certificate SHA-256 (includes rotation history when available):\nUnavailable"))
    }
    @Test fun malformedArchiveFailsClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("invalid-", ".apk", context.cacheDir)
        try {
            file.writeText("Not an APK")
            assertThrows(Exception::class.java) { ApkInspector.inspect(context, file) }
        } finally { file.delete() }
    }
}
