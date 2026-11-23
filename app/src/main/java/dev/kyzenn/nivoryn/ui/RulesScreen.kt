package dev.kyzenn.nivoryn.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.data.IndicatorStore
import dev.kyzenn.nivoryn.ui.components.*
import kotlinx.coroutines.*

@Composable fun RulesScreen() {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    val store=remember { IndicatorStore(context) }
    var status by remember { mutableStateOf("Reading local pack…") }
    var busy by remember { mutableStateOf(false) }
    suspend fun refresh() { status=withContext(Dispatchers.IO) {
        store.load()?.let { "${it.source}\nVersion ${it.version} · ${if(it.fresh(System.currentTimeMillis())) "current" else "stale; matches disabled"}\nExpires: ${java.util.Date(it.expires)}" }
            ?: "No trusted indicator pack installed. Rules and structure checks remain available; no reputation-list match can be claimed."
    } }
    LaunchedEffect(Unit) { refresh() }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO) {
                    val text=context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                        while(true) { ensureActive(); val n=input.read(buffer); if(n<0) break; require(bytes.size()+n<=1_500_000); bytes.write(buffer,0,n) }
                        bytes.toString("UTF-8")
                    } ?: error("Cannot read resource")
                    store.install(text)
                }
                refresh()
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { status="Pack rejected. A current signed pack and a separately pinned publisher key are required. Existing indicators were kept." }
            finally { busy=false }
        }
    }
    PageTitle("Rules and indicators", "Offline resources · authenticated updates")
    Text(status)
    Text("Built-in text rules run without an indicator pack. Imported domain/APK/certificate lists are checked only when their publisher signature and freshness pass. Membership is evidence, not a verdict.")
    if(!busy) {
        TextAction("Import signed indicator pack", { picker.launch(arrayOf("application/json","application/octet-stream")) })
        TextAction("Restore previous trusted pack", { scope.launch {
            busy=true
            try { withContext(Dispatchers.IO) { store.rollback() }; refresh() }
            catch(e:CancellationException) { throw e }
            catch(_:Exception) { status="No current, signed previous pack is available." }
            finally { busy=false }
        } })
        TextAction("Remove imported indicator packs", { scope.launch {
            withContext(Dispatchers.IO) { store.deleteAll() }; refresh()
        } })
    } else Text("Verifying locally…")
    Text("No publisher keys or real-world threat list are bundled in this development build. Arbitrary imports fail closed. An untrusted sidecar checksum or embedded public key cannot authorise a pack.")
}
