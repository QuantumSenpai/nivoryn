package dev.kyzenn.nivoryn.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import dev.kyzenn.nivoryn.domain.CheckDraft
import dev.kyzenn.nivoryn.integration.*
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun ScanScreen(initialKind:String,onCaptured:(CheckDraft)->Unit,onImport:(String)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val preferences=remember { context.getSharedPreferences("scan-ui",0) }
    var pending by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(if(initialKind=="QR") "QR" else "Screenshot") }
    var error by rememberSaveable { mutableStateOf("") }
    var reading by remember { mutableStateOf(false) }
    val capture=rememberLauncherForActivityResult(ScanCaptureContract()) { success ->
        val uri=pending; pending=""
        if(success && uri.isNotEmpty()) scope.launch {
            reading=true
            try {
                val draft=withContext(Dispatchers.IO) { ShareInputParser.attachment(context,uri.toUri(),kind) }
                require(draft.bytes>0) { "The camera returned an empty image." }
                context.revokeUriPermission(uri.toUri(),Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                onCaptured(draft)
            } catch(e:CancellationException) { ScanFiles.release(context,uri); throw e }
            catch(_:Exception) { ScanFiles.release(context,uri); error="The image could not be read. Retry with a smaller image or import from files (20 MB limit)." }
            finally { reading=false }
        } else { ScanFiles.release(context,uri); error="Camera closed without a capture. Retry or import an image." }
    }
    fun launchCamera() {
        if(reading || pending.isNotEmpty()) return
        reading=true
        scope.launch {
            runCatching {
                val uri=withContext(Dispatchers.IO) { ScanFiles.create(context) }
                pending=uri.toString()
                capture.launch(uri)
            }.onFailure { ScanFiles.release(context,pending); pending=""; error="Camera unavailable or insufficient storage. Import an image instead." }
            reading=false
        }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) launchCamera() else error="Camera permission denied. Import an image instead, or enable Camera in Android app settings."
    }
    PageTitle("Scan content","Capture a page, message or QR. Review it before analysis.")
    GroupedSurface("What to read") {
        SettingSwitch("QR only","Decode the original payload without opening its destination.",kind=="QR",{kind=if(it) "QR" else "Screenshot"})
        Text(if(kind=="QR") "QR decoding is local. No link or payment request opens automatically." else "Bundled Latin/Devanagari OCR and QR decoding run on this device.",style=MaterialTheme.typography.bodyMedium)
    }
    GroupedSurface("Capture or import") {
        ActionButton(if(reading) "Preparing image…" else "Open camera",{
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) launchCamera()
            else if(preferences.getBoolean("asked",false)) error="Camera permission is disabled. Use Import image or open Android app settings."
            else { preferences.edit { putBoolean("asked",true) }; permission.launch(Manifest.permission.CAMERA) }
        },Modifier.fillMaxWidth(),enabled=!reading && pending.isEmpty())
        ActionButton("Import image",{onImport(kind)},Modifier.fillMaxWidth(),secondary=true,enabled=!reading)
        Text("Use even light, keep text in focus and include the whole QR. Images are orientation-corrected and bounded before processing.",style=MaterialTheme.typography.bodySmall)
        if(error.isNotEmpty()) {
            Notice(error)
            TextAction("Open app permissions",{context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,"package:${context.packageName}".toUri()))})
        }
    }
    Notice("You will review the image and extracted text next. Capture does not start an analysis. Temporary camera images are removed when their attachment is cleared or replaced; unused captures older than two days are cleaned when another capture starts.")
}
