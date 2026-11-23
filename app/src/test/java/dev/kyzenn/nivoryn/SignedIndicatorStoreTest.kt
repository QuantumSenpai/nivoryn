package dev.kyzenn.nivoryn
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.data.IndicatorStore
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.*
import java.util.Base64
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class SignedIndicatorStoreTest {
    @Test fun rejectsTamperingUnknownKeysAndSilentDowngrade() {
        val pair=KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val keys=JSONObject().put("test",Base64.getEncoder().encodeToString(pair.public.encoded)).toString()
        val store=IndicatorStore(ApplicationProvider.getApplicationContext<Context>()) { keys }
        fun envelope(version: Int): String {
            val payload=JSONObject().put("schema",1).put("source","Synthetic fixture").put("licence","CC0 test fixture").put("version",version)
                .put("issued",1000).put("expires",2000).put("domains",JSONArray().put("bad.example"))
                .put("apkSha256",JSONArray()).put("certificateSha256",JSONArray()).toString().toByteArray()
            val sig=Signature.getInstance("SHA256withRSA").apply { initSign(pair.private); update(payload) }.sign()
            return JSONObject().put("keyId","test").put("payload",Base64.getEncoder().encodeToString(payload)).put("signature",Base64.getEncoder().encodeToString(sig)).toString()
        }
        val first=envelope(1); store.install(first,1500)
        store.install(envelope(2),1500)
        assertEquals(2L,store.load()?.version)
        assertThrows(Exception::class.java) { store.install(first,1500) }
        val bad=JSONObject(envelope(3)).put("payload",Base64.getEncoder().encodeToString("{}".toByteArray())).toString()
        assertThrows(Exception::class.java) { store.install(bad,1500) }
        assertEquals(2L,store.load()?.version)
        store.rollback(1500); assertEquals(1L,store.load()?.version)
        store.deleteAll(); assertNull(store.load())
    }
}
