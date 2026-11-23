package dev.kyzenn.nivoryn
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import dev.kyzenn.nivoryn.detection.SpamClassifier
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class SpamClassifierTest {
    @Test fun kotlinScoresMatchTrainingFeaturePipeline() {
        val model=SpamClassifier.load(ApplicationProvider.getApplicationContext<Context>())
        val cases=JSONArray(javaClass.getResourceAsStream("/classifier-golden.json")!!.bufferedReader().readText())
        for(i in 0 until cases.length()) {
            val row=cases.getJSONObject(i)
            assertEquals(row.getString("text"),row.getDouble("probability"),model.probability(row.getString("text")),1e-10)
        }
    }
    @Test fun unknownWordsCannotBecomeAConfidentPrediction() {
        val model=SpamClassifier.load(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(0.0,model.probability("zzxyzqxx qqzxxy"),0.0)
    }
}
