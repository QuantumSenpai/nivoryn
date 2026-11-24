package dev.kyzenn.nivoryn.integration

import android.content.*
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.util.UUID

object ScanFiles {
    private val filename=Regex("capture-[a-f0-9-]{36}\\.jpg")
    fun create(context:Context):Uri {
        val directory=File(context.cacheDir,"scans").apply { mkdirs() }
        directory.listFiles().orEmpty().filter { filename.matches(it.name) && System.currentTimeMillis()-it.lastModified()>172800000 }.forEach { it.delete() }
        require(directory.listFiles().orEmpty().size<8) { "Clear old scan drafts before capturing another image." }
        require(android.os.StatFs(directory.path).availableBytes>64_000_000) { "Not enough space for a scan. Import a smaller image instead." }
        val file=File(directory,"capture-${UUID.randomUUID()}.jpg").apply { createNewFile() }
        return FileProvider.getUriForFile(context,context.packageName+".scan-files",file)
    }
    fun release(context:Context,value:String) {
        if(value.isBlank()) return
        val uri=value.toUri()
        if(uri.authority==context.packageName+".scan-files") {
            val name=uri.lastPathSegment.orEmpty()
            if(filename.matches(name)) {
                val file=File(context.cacheDir,"scans/$name")
                if(FileProvider.getUriForFile(context,context.packageName+".scan-files",file)==uri) file.delete()
            }
            context.revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } else runCatching { context.contentResolver.releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}

class ScanCaptureContract:androidx.activity.result.contract.ActivityResultContracts.TakePicture() {
    override fun createIntent(context:Context,input:Uri):Intent = super.createIntent(context,input).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        clipData=ClipData.newRawUri("Nivoryn scan",input)
    }
}
