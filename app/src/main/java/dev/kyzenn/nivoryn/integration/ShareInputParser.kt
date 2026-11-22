package dev.kyzenn.nivoryn.integration

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import dev.kyzenn.nivoryn.domain.CheckDraft

object ShareInputParser {
    fun text(intent: Intent?): String = if(intent?.action == Intent.ACTION_SEND && intent.type == "text/plain")
        intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.take(12000).orEmpty() else ""
    fun attachment(context: Context, uri: Uri, kind: String): CheckDraft {
        require(uri.scheme == "content") { "Choose a file through the Android document picker." }
        var name="Selected file"; var size=-1L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
            if(it.moveToFirst()) { name=it.getString(0) ?: name; if(!it.isNull(1)) size=it.getLong(1) }
        }
        require(size <= 20 * 1024 * 1024) { "Choose a file smaller than 20 MB." }
        return CheckDraft(attachmentUri=uri.toString(), filename=name.filterNot { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }.take(300), bytes=size,
            mime=context.contentResolver.getType(uri) ?: "application/octet-stream", kind=kind)
    }
}
