package dev.kyzenn.nivoryn

import android.graphics.*
import android.security.keystore.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.kyzenn.nivoryn.data.HistoryCipher
import dev.kyzenn.nivoryn.domain.*
import org.junit.*
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class OfflineDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun localEngineFlowWorksInTheActualAndroidProcess() {
        val arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        val requireLlama = arguments.getString("llamaRequired") == "true"
        val recording = arguments.getString("recording") == "true"
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Start checking").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Review input").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Start checking").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Start checking").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Please send your OTP to finish verification.")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithText("Review input").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Run local check").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Run local check").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("!  High risk").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Local check · Evidence-led; signals are not a verdict").assertExists()
        if (requireLlama) {
            compose.onNodeWithText("Llama generated explanation · validated references").performScrollTo().assertIsDisplayed()
            if (recording) { compose.waitForIdle(); Thread.sleep(3000) }
        }
        compose.onNodeWithText("View evidence").performScrollTo().performClick()
        compose.onNodeWithText("A secret is requested").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Source: Reviewed text", substring = true).performScrollTo().assertIsDisplayed()
        // Recording-only pause makes real rendered evidence readable; production has no delay.
        if (recording) {
            compose.waitForIdle()
            Thread.sleep(8000)
        }
        if (requireLlama) {
            compose.onNodeWithText("Save check locally").performScrollTo().performClick()
            compose.onNodeWithText("History").performClick()
            val original = "Please send your OTP to finish verification."
            compose.waitUntil(10000) { compose.onAllNodesWithText(original).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(original).performScrollTo().performClick()
            compose.onNodeWithText("Llama generated explanation · validated references").performScrollTo().assertIsDisplayed()
            if (!recording) {
                compose.onNodeWithText("Save check locally").performScrollTo().performClick()
                compose.onNodeWithText("Llama generated explanation · validated references").performScrollTo().assertIsDisplayed()
                compose.activityRule.scenario.recreate()
                compose.waitUntil(10000) { compose.onAllNodesWithText("Llama generated explanation · validated references").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Llama generated explanation · validated references").assertExists()
            }
            if (recording) { compose.waitForIdle(); Thread.sleep(3000) }
            compose.onNodeWithText("History").performClick()
            compose.onNodeWithText("Delete saved check").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText(original).fetchSemanticsNodes().isEmpty() }
            if (recording) { compose.waitForIdle(); Thread.sleep(2000) }
        }
    }

    @Test fun bundledOcrReadsRealBitmapAndFeedsLocalEvidence() {
        val bitmap = Bitmap.createBitmap(900, 220, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawText("Share your OTP now", 32f, 130f, paint)
        val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val recognized = Tasks.await(client.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS).text
            Assert.assertTrue(recognized, recognized.contains("OTP", ignoreCase = true))
            val result = AnalyseInput().analyse(CheckDraft(text = recognized, kind = "Screenshot"))
            Assert.assertEquals(Assessment.HIGH, result.assessment)
            Assert.assertTrue(result.evidence.all { recognized.contains(it.quote) })
        } finally { client.close(); bitmap.recycle() }
    }

    @Test fun androidKeystoreKeyEncryptsAndRejectsTamperedHistory() {
        val alias = "nivoryn-test-${java.util.UUID.randomUUID()}"
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val key = KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build())
            }.generateKey()
            Assert.assertNull("Keystore key material must not be exportable", key.encoded)
            val cipher = HistoryCipher(key); val plain = "Synthetic private test message".toByteArray()
            val encrypted = cipher.encrypt(plain)
            Assert.assertArrayEquals(plain, cipher.decrypt(encrypted))
            encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
            Assert.assertThrows(Exception::class.java) { cipher.decrypt(encrypted) }
        } finally { keys.deleteEntry(alias) }
    }
}
