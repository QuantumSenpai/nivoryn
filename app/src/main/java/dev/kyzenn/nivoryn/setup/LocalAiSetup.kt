package dev.kyzenn.nivoryn.setup

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.content.edit
import androidx.core.net.toUri
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.reasoning.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

object SupportedModelDownload {
    const val BASE="https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET/resolve/911221e0f49d3354cbd4697a9c09feee25cadec7/"
    fun url(name:String):String {
        ModelStore.requireName(name)
        return BASE+if(name=="model") "Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte" else "tokenizer.model"
    }
    fun ready(mode:String)=mode.startsWith("Llama generated explanation")
    fun requireSpace(downloadFree:Long,privateFree:Long,total:Long) {
        require(downloadFree>=total*2+256_000_000 && privateFree>=total+128_000_000) { "Allow at least 2.6 GB free for download and verified private installation, plus existing resources." }
    }
}

data class SetupState(val phase:String="Inspecting",val detail:String="Checking device and installed resources…",val downloaded:Long=0,val total:Long=1138135470,val installed:Boolean=false,val ready:Boolean=false,val elapsedMs:Long=0) {
    val busy:Boolean get()=phase in setOf("Inspecting","Downloading","Waiting for network","Verifying","Testing","Importing","Deleting")
}

class LocalAiSetup(private val context:Context,private val runner:LlamaRunner) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val preferences=context.getSharedPreferences("local-ai-setup",0)
    private val manager=context.getSystemService(DownloadManager::class.java)
    private val store=ModelStore(context)
    private var operation:Job?=null
    val state=MutableStateFlow(SetupState())
    init {
        operation=scope.launch {
            try {
                if(downloadIds().isNotEmpty()) observeDownloads()
                else refresh()
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { state.value=SetupState("Needs setup","Resources could not be verified. Retry setup or import the supported pair.") }
        }
    }
    private fun downloadIds()=listOf("model","tokenizer").mapNotNull { name -> preferences.getLong(name,-1).takeIf { it>=0 }?.let { name to it } }.toMap()
    private suspend fun refresh() {
        val installed=withContext(Dispatchers.IO) { store.installed()!=null }
        val ready=installed && preferences.getString("inference-profile","")==store.preferred().id
        state.value=SetupState(if(ready) "Ready for offline explanations" else if(installed) "Test required" else "Needs setup",
            if(ready) "Pinned resources verified. A real Llama inference check passed on this installation." else if(installed) "Resources verified. Run the real inference check to establish readiness." else "Basic checks work now. Set up Llama for on-device explanations.",installed=installed,ready=ready)
    }
    fun download(allowMobile:Boolean,licenceReviewed:Boolean) {
        if(state.value.busy) return
        operation=scope.launch {
            try {
                require(licenceReviewed) { "Review the Llama licence and acceptable use policy before downloading." }
                require(Build.SUPPORTED_ABIS.contains("arm64-v8a")) { "This model requires an Arm64 Android device. Basic checks remain available." }
                state.value=SetupState("Inspecting","Checking storage before downloading…")
                withContext(Dispatchers.IO) {
                    val directory=requireNotNull(context.getExternalFilesDir("model-downloads")).apply { mkdirs() }
                    val total=store.preferred().modelBytes+store.preferred().tokenBytes
                    SupportedModelDownload.requireSpace(android.os.StatFs(directory.path).availableBytes,android.os.StatFs(context.noBackupFilesDir.path).availableBytes,total)
                    cleanupDownloads()
                    for(name in listOf("model","tokenizer")) {
                        val file=File(directory,"$name.download")
                        if(file.exists()) check(file.delete()) { "Could not replace the previous download" }
                        val request=DownloadManager.Request(SupportedModelDownload.url(name).toUri())
                            .setTitle("Nivoryn local AI · $name").setDescription("Pinned Llama 3.2 resource. Analysis remains on-device.")
                            .setAllowedOverMetered(allowMobile).setAllowedOverRoaming(false)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                            .setDestinationUri(Uri.fromFile(file))
                        val id=manager.enqueue(request)
                        preferences.edit(commit=true) { putLong(name,id) }
                    }
                }
                observeDownloads()
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { withContext(Dispatchers.IO) { cleanupDownloads() }; fail(e.message?:"Download could not start. Check network, storage and publisher access.") }
        }
    }
    private suspend fun observeDownloads() {
        val ids=downloadIds()
        require(ids.keys==setOf("model","tokenizer")) { "Interrupted setup did not enqueue both files. Cancel and retry." }
        while(currentCoroutineContext().isActive) {
            var bytes=0L
            var complete=0
            var paused=false
            withContext(Dispatchers.IO) {
                for((name,id) in ids) manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                    require(cursor!=null && cursor.moveToFirst()) { "Download was removed. Retry setup." }
                    val status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val count=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    require(count<=store.preferred().size(name)) { "Publisher bytes exceed the trusted resource size. Download refused." }
                    bytes+=count.coerceAtLeast(0)
                    if(status==DownloadManager.STATUS_FAILED) error("Download failed (Android code ${cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))}). Check connectivity, free space and publisher access, then retry.")
                    if(status==DownloadManager.STATUS_PAUSED) paused=true
                    if(status==DownloadManager.STATUS_SUCCESSFUL) complete++
                }
            }
            state.value=SetupState(if(paused) "Waiting for network" else "Downloading",if(paused) "Android paused the transfer. It resumes automatically when network/storage conditions allow." else "Downloading the pinned model and tokenizer from the upstream-linked publisher.",downloaded=bytes)
            if(complete==2) break
            delay(1000)
        }
        state.value=SetupState("Verifying","Verifying exact sizes and SHA-256, then preparing the private resource pair…")
        val job=currentCoroutineContext().job
        runner.manageResources {
            for((name,id) in ids) ModelImporter(store).import(name,ParcelFileDescriptor.AutoCloseInputStream(manager.openDownloadedFile(id))) { !job.isActive }
        }
        withContext(Dispatchers.IO) { cleanupDownloads() }
        testInference()
    }
    fun test() {
        if(state.value.busy) return
        operation=scope.launch {
            try { testInference() }
            catch(e:CancellationException) { state.value=SetupState("Test cancelled","Resources may remain installed. Retry the inference check."); throw e }
            catch(e:Exception) { fail(e.message?:"Local inference failed. No readiness claim was made.") }
        }
    }
    private suspend fun testInference() {
        preferences.edit { remove("inference-profile") }
        state.value=SetupState("Testing","Loading Llama and running a real, synthetic on-device explanation…",installed=true)
        val started=SystemClock.elapsedRealtime()
        val finding=withContext(Dispatchers.Default) { AnalyseInput().analyse(CheckDraft(text="Please send your OTP to finish verification.")) }
        val result=runner.explain(finding)
        val ready=SupportedModelDownload.ready(result.explanationMode)
        if(ready) preferences.edit { putString("inference-profile",store.preferred().id) }
        state.value=SetupState(if(ready) "Ready for offline explanations" else "Inference check failed",
            "${result.explanationMode}. ${if(ready) "You can now disconnect and run checks offline." else "Curated guidance remains available; setup is not marked ready."}",installed=withContext(Dispatchers.IO) { store.installed()!=null },ready=ready,elapsedMs=SystemClock.elapsedRealtime()-started)
    }
    fun import(name:String,uri:Uri) {
        if(state.value.busy) return
        operation=scope.launch {
            try {
                state.value=SetupState("Importing","Verifying the selected $name against the pinned release…")
                preferences.edit { remove("inference-profile") }
                val job=currentCoroutineContext().job
                runner.manageResources { ModelImporter(store).import(name,requireNotNull(context.contentResolver.openInputStream(uri))) { !job.isActive } }
                refresh()
            } catch(e:CancellationException) { state.value=SetupState("Import cancelled","Verified partial imports stay private and can be reselected to resume."); throw e }
            catch(e:Exception) { fail(e.message?:"Import refused. Select the exact supported pair.") }
        }
    }
    fun cancel() {
        val previous=operation
        previous?.cancel()
        operation=scope.launch {
            previous?.join()
            withContext(Dispatchers.IO) { cleanupDownloads() }
            refresh()
            state.value=state.value.copy(detail="Operation cancelled. Downloaded copies were removed; verified installed resources were kept.")
        }
    }
    fun delete() {
        if(state.value.busy) return
        operation=scope.launch {
            state.value=SetupState("Deleting","Removing active, previous and partial model resources…")
            try {
                withContext(Dispatchers.IO) { cleanupDownloads() }
                runner.manageResources { store.deleteAll() }
                preferences.edit { remove("inference-profile") }
                refresh()
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { fail("Some resources could not be removed. Retry deletion before assuming they are gone.") }
        }
    }
    fun rollback() {
        if(state.value.busy) return
        operation=scope.launch {
            try { state.value=SetupState("Verifying","Restoring the previous trusted resource pair…"); runner.manageResources { store.rollback() }; preferences.edit { remove("inference-profile") }; refresh() }
            catch(e:CancellationException) { throw e }
            catch(_:Exception) { fail("A complete previous trusted pair is unavailable. Current resources were kept.") }
        }
    }
    private fun cleanupDownloads() {
        val ids=downloadIds().values.toLongArray()
        if(ids.isNotEmpty()) manager.remove(*ids)
        preferences.edit(commit=true) { remove("model"); remove("tokenizer") }
    }
    private suspend fun fail(message:String) {
        val installed=withContext(Dispatchers.IO) { store.installed()!=null }
        val ready=installed && preferences.getString("inference-profile","")==store.preferred().id
        state.value=SetupState("Setup needs attention",message.take(300)+(if(installed) " Existing verified resources were kept; you can retest them." else ""),installed=installed,ready=ready)
    }
}
