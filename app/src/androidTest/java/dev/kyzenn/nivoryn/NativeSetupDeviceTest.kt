package dev.kyzenn.nivoryn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import dev.kyzenn.nivoryn.integration.*
import dev.kyzenn.nivoryn.network.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL

@RunWith(AndroidJUnit4::class)
class NativeSetupDeviceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context:Context=instrumentation.targetContext
    @Test fun cameraContentUriHasExplicitGrantsAndRealLocalQrDecode()=runBlocking {
        val uri=ScanFiles.create(context)
        try {
            val intent=ScanCaptureContract().createIntent(context,uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
            assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION!=0)
            assertEquals(uri,intent.clipData?.getItemAt(0)?.uri)
            val payload="https://example.org/verify-camera"
            val qr=MultiFormatWriter().encode(payload,BarcodeFormat.QR_CODE,320,320)
            val bitmap=Bitmap.createBitmap(320,320,Bitmap.Config.ARGB_8888)
            for(y in 0 until 320) for(x in 0 until 320) bitmap.setPixel(x,y,if(qr[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            context.contentResolver.openOutputStream(uri)!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)) }
            bitmap.recycle()
            val start=SystemClock.elapsedRealtime()
            val draft=ShareInputParser.attachment(context,uri,"QR")
            val result=LocalExtraction.readDetailed(context,draft,false)
            assertEquals(payload,result.qr)
            File(context.filesDir,"camera-uri-evidence.json").writeText(JSONObject().put("contentUri",true).put("explicitGrants",true).put("decodedWithoutOpening",true).put("qrMs",SystemClock.elapsedRealtime()-start).toString())
        } finally { ScanFiles.release(context,uri.toString()) }
    }
    @Test fun liveUsageObservesCountersWithoutReroutingOrBreakingHttps() {
        if(Build.VERSION.SDK_INT>=33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName,Manifest.permission.POST_NOTIFICATIONS)
        ActivityScenario.launch(MainActivity::class.java).use {
            val connectivity=context.getSystemService(ConnectivityManager::class.java)
            val before=connectivity.activeNetwork
            assertNotNull("Enable a test network for observation acceptance",before)
            try {
                ContextCompat.startForegroundService(context,Intent(context,UsageMonitorService::class.java))
                val deadline=SystemClock.elapsedRealtime()+15000
                while(UsageObservation.state.value.samples.isEmpty() && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(250)
                assertTrue(UsageObservation.state.value.running)
                assertTrue(UsageObservation.state.value.samples.isNotEmpty())
                val connection=URL("https://nivoryn.vercel.app/release.json").openConnection() as javax.net.ssl.HttpsURLConnection
                connection.connectTimeout=10000;connection.readTimeout=10000
                try { assertEquals(200,connection.responseCode);connection.inputStream.use { it.readBytes() } } finally { connection.disconnect() }
                assertEquals(before,connectivity.activeNetwork)
                val properties=connectivity.getLinkProperties(before)
                val report=JSONObject().put("running",true).put("httpsConnectivityPreserved",true).put("sameNetworkHandle",true)
                    .put("sampleCount",UsageObservation.state.value.samples.size).put("dnsServerCount",properties?.dnsServers?.size?:0)
                    .put("ipv6AddressPresent",properties?.linkAddresses?.any { address->address.address is java.net.Inet6Address }?:false)
                    .put("scope","Aggregate usage only; no destination classification, packet interception or blocking")
                val notification=context.getSystemService(android.app.NotificationManager::class.java).activeNotifications.single { it.id==42 }.notification
                assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT!=0)
                notification.actions.single { action->action.title.toString()=="Stop" }.actionIntent.send()
                report.put("ongoingNotification",true).put("notificationStopActionSent",true)
                File(context.filesDir,"live-usage-evidence.json").writeText(report.toString())
            } finally { context.stopService(Intent(context,UsageMonitorService::class.java)) }
            val deadline=SystemClock.elapsedRealtime()+5000
            while(UsageObservation.state.value.running && SystemClock.elapsedRealtime()<deadline) SystemClock.sleep(50)
            assertFalse(UsageObservation.state.value.running)
        }
    }
}
