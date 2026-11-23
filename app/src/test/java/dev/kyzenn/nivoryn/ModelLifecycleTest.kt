package dev.kyzenn.nivoryn
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.reasoning.ModelImporter
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class ModelLifecycleTest {
    private fun hash(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
    private fun profile(v: String, model: ByteArray, token: ByteArray) = JSONObject().put("version",v).put("family","Llama")
        .put("runtime","executorch-1.2.0").put("modelSha256",hash(model)).put("tokenizerSha256",hash(token))
        .put("modelBytes",model.size).put("tokenizerBytes",token.size)
    @Test fun activatesOnlyCompletePairsAndCanRollbackOrDelete() {
        val a="model-a".toByteArray(); val b="model-b".toByteArray(); val t="tokenizer".toByteArray()
        var manifest=profile("a",a,t).toString()
        val store=ModelStore(ApplicationProvider.getApplicationContext<Context>()) { manifest }
        val importer=ModelImporter(store)
        importer.import("model",a.inputStream()); assertNull(store.installed())
        importer.import("tokenizer",t.inputStream()); assertEquals("a",store.installed()?.version)
        manifest=JSONObject().put("models",JSONArray().put(profile("a",a,t)).put(profile("b",b,t))).toString()
        importer.import("model",b.inputStream()); assertEquals("a",store.installed()?.version)
        assertThrows(Exception::class.java) { importer.import("tokenizer","bad".byteInputStream()) }
        assertEquals("a",store.installed()?.version)
        importer.import("tokenizer",t.inputStream()); assertEquals("b",store.installed()?.version)
        store.rollback(); assertEquals("a",store.installed()?.version)
        store.deleteAll(); assertNull(store.installed())
    }
    @Test fun interruptedImportCanBeReselectedWithoutActivatingPartialData() {
        val model=ByteArray(100000) { (it % 251).toByte() }; val token=byteArrayOf(1)
        val manifest=profile("resume",model,token).toString()
        val store=ModelStore(ApplicationProvider.getApplicationContext<Context>()) { manifest }
        val broken=object: java.io.InputStream() {
            var count=0
            override fun read():Int { if(count==70000) throw java.io.IOException("interruption"); return if(count<model.size) model[count++].toInt() and 255 else -1 }
        }
        val importer=ModelImporter(store)
        assertThrows(Exception::class.java) { importer.import("model",broken) }
        assertNull(store.installed())
        importer.import("model",model.inputStream()); importer.import("tokenizer",token.inputStream())
        assertNotNull(store.installed())
    }
    @Test fun cancellationAtEofCannotActivateACompletePair() {
        val model="complete-model".toByteArray(); val token=byteArrayOf(7)
        val store=ModelStore(ApplicationProvider.getApplicationContext<Context>()) { profile("cancel",model,token).toString() }
        val importer=ModelImporter(store)
        importer.import("tokenizer",token.inputStream())
        var cancelled=false
        val input=object: java.io.ByteArrayInputStream(model) {
            override fun read(b:ByteArray, off:Int, len:Int):Int = super.read(b,off,len).also { if(it<0) cancelled=true }
        }
        assertThrows(java.util.concurrent.CancellationException::class.java) { importer.import("model", input) { cancelled } }
        assertNull(store.installed())
        importer.import("model",model.inputStream())
        assertEquals("cancel",store.installed()?.version)
    }
}
