package dev.kyzenn.nivoryn

import android.app.Application
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.reasoning.LlamaRunner

class NivorynApplication : Application() {
    val spam by lazy { dev.kyzenn.nivoryn.detection.SpamClassifier.load(this) }
    val llama by lazy { LlamaRunner(ModelStore(this)) }
}
