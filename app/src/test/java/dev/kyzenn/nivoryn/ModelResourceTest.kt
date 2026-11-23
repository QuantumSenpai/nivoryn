package dev.kyzenn.nivoryn
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.reasoning.ModelImporter
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
@org.robolectric.annotation.Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ModelResourceTest {
    @Test fun unsupportedInputIsNeverExplainedAsASuccessfulCheck() {
        val store=ModelStore(ApplicationProvider.getApplicationContext<Context>())
        dev.kyzenn.nivoryn.reasoning.LlamaRunner(store).use { runner ->
            val unknown=dev.kyzenn.nivoryn.domain.AnalyseInput().analyse(dev.kyzenn.nivoryn.domain.CheckDraft())
            val result=kotlinx.coroutines.runBlocking { runner.explain(unknown) }
            Assert.assertEquals(unknown.explanation,result.explanation)
            Assert.assertEquals("Curated fallback · Insufficient supported evidence",result.explanationMode)
        }
    }
    @Test fun refusesUntrustedImportsWithoutReplacingResources() {
        val store = ModelStore(ApplicationProvider.getApplicationContext<Context>())
        Assert.assertNull(store.installed())
        Assert.assertThrows(Exception::class.java) { ModelImporter(store).import("model", ByteArrayInputStream(byteArrayOf(1, 2))) }
        Assert.assertNull(store.installed())
    }
}
