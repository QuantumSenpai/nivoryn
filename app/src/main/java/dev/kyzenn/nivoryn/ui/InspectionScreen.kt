package dev.kyzenn.nivoryn.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.integration.LocalInspection
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun InspectionScreen(network: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var result by androidx.compose.runtime.saveable.rememberSaveable(network) { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) job = scope.launch {
            result = "Inspecting locally…"
            try {
                result = LocalInspection.inspect(context, uri, network)
            } catch (_: TimeoutCancellationException) { result = "Inspection timed out. No safety conclusion was made."
            } catch (e: CancellationException) { result = "Inspection cancelled."; throw e
            } catch (_: Exception) { result = "Input rejected or unsupported. Use a valid classic PCAP or APK under 20 MB. No safety conclusion was made." }
            finally { job = null }
        }
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    PageTitle(if (network) "Network replay" else "APK indicators")
    Text(if (network) "Import a classic PCAP captured on a device you control. Ethernet/raw IPv4 TCP and UDP are supported. IPv6, VLAN, fragments and PCAPNG are not analysed. No TLS plaintext is available."
        else "Import an APK to inspect its package identity, signing certificates and declared permissions. The file is never installed or executed.")
    Text(if (network) "Live Android monitoring is unavailable: no VPN is registered or started. Threshold signals are experimental and can flag legitimate administration."
        else "Static indicators cannot establish malware or observe runtime behaviour. Unknown files remain untrusted.")
    if (job == null) TextAction(if (network) "Import PCAP" else "Import APK", { picker.launch(arrayOf("*/*")) })
    else TextAction("Cancel inspection", { job?.cancel() })
    if (result.isNotEmpty()) Text(result)
}
