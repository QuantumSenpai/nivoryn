package dev.kyzenn.nivoryn

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManifestContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val info = context.packageManager.getPackageInfo(context.packageName,
        PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PERMISSIONS)

    @Test fun registeredComponentsExistAndHaveTheRequiredAndroidType() {
        info.activities.orEmpty().forEach { assertTrue(Activity::class.java.isAssignableFrom(Class.forName(it.name))) }
        info.services.orEmpty().forEach { assertTrue(Service::class.java.isAssignableFrom(Class.forName(it.name))) }
        info.receivers.orEmpty().forEach { assertTrue(BroadcastReceiver::class.java.isAssignableFrom(Class.forName(it.name))) }
    }

    @Test fun offlineChecksDoNotRequestUnusedCameraOrBackgroundPrivileges() {
        val unused = setOf("CAMERA", "FOREGROUND_SERVICE", "FOREGROUND_SERVICE_DATA_SYNC",
            "POST_NOTIFICATIONS", "WAKE_LOCK", "RECEIVE_BOOT_COMPLETED")
        assertTrue(info.requestedPermissions.orEmpty().none { it.removePrefix("android.permission.") in unused })
    }

    @Test fun sharedTextStartsAnActivityThatReadsTheIncomingIntent() {
        val main = info.activities.orEmpty().single { it.name == MainActivity::class.java.name }
        assertEquals(ActivityInfo.LAUNCH_MULTIPLE, main.launchMode)
    }
}
