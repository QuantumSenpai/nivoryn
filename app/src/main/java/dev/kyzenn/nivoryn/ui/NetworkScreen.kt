package dev.kyzenn.nivoryn.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dev.kyzenn.nivoryn.network.*
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun NetworkScreen(onCapture:()->Unit) {
    val context=LocalContext.current
    val state by UsageObservation.state.collectAsState()
    var consent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val preferences=remember { context.getSharedPreferences("network-ui",0) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        error=if(granted) "Notifications enabled. Select Start observation to continue." else "Notifications denied. Enable them in Android app settings to start observation, or import a capture."
    }
    PageTitle("Network activity","Live usage observation and separate capture analysis.")
    GroupedSurface("Live device usage") {
        Text(state.message,Modifier.semantics { liveRegion=LiveRegionMode.Polite },style=MaterialTheme.typography.titleSmall)
        Text("Observes system byte totals every 5 seconds. Up to 60 samples stay in memory. No message content, packet payloads, IP addresses or DNS queries are collected.",style=MaterialTheme.typography.bodyMedium)
        if(state.running) ActionButton("Stop observation",{context.stopService(Intent(context,UsageMonitorService::class.java))},Modifier.fillMaxWidth())
        else ActionButton("Start observation",{
            if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
                if(preferences.getBoolean("asked",false)) error="Notifications are disabled. Enable them in Android app settings, or use capture import."
                else { preferences.edit { putBoolean("asked",true) }; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else consent=true
        },Modifier.fillMaxWidth())
        if(error.isNotBlank()) {
            Notice(error)
            TextAction("Open notification settings",{context.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))})
        }
        Text("Traffic-volume observations can reflect downloads, streaming or updates. They are not threat verdicts. IPv4 and IPv6 are counted together where supported by Android; DNS and encrypted content are not inspected.",style=MaterialTheme.typography.bodySmall)
    }
    if(state.samples.isNotEmpty()) GroupedSurface("Recent usage · memory only") {
        if(!state.running) TextAction("Clear usage samples",{UsageObservation.state.value=UsageState()})
        state.samples.takeLast(6).reversed().forEach { sample ->
            Text("↓ ${sample.received/1024} KB  ↑ ${sample.sent/1024} KB · ${sample.bytesPerSecond/1024} KB/s",style=MaterialTheme.typography.titleSmall)
            Text("${sample.transport} · ${sample.note}",style=MaterialTheme.typography.bodySmall)
        }
    }
    GroupedSurface("Capture import") {
        ActionRow("Inspect network capture","Classic IPv4 PCAP replay with bounded flow/scan indicators. This is not live monitoring.",onCapture,glyph=3)
    }
    GroupedSurface("Visibility and control") {
        Text("No VPN is installed and no traffic is rerouted. Existing VPNs can remain connected; their accounting may overlap. Live destination-level threat detection, TLS inspection and blocking are unavailable.")
        Text("Observation stops if Android kills the process; it never restarts at boot. Return here to restart explicitly. Wi-Fi/mobile changes establish a new visible transport label without changing connectivity.",style=MaterialTheme.typography.bodySmall)
    }
    if(consent) AlertDialog(onDismissRequest={consent=false},title={Text("Observe network usage?")},text={Text("Nivoryn will show device-wide byte totals in a foreground observation session with a Stop notification. It cannot identify destinations or protect all traffic. Nothing is uploaded or saved to disk.")},
        confirmButton={TextAction("Start observation",{
            consent=false; error=""
            runCatching { ContextCompat.startForegroundService(context,Intent(context,UsageMonitorService::class.java)) }.onFailure { error="Android did not allow observation to start. Keep the app visible and retry." }
        })},dismissButton={TextAction("Cancel",{consent=false})})
}
