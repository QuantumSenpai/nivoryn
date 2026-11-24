package dev.kyzenn.nivoryn

import android.app.Application
import kotlinx.coroutines.launch
import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.reasoning.LlamaRunner

class NivorynApplication : Application() {
    private val memoryScope=kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.Main.immediate)
    private val localRunner=lazy { LlamaRunner(ModelStore(this)) }
    override fun onTrimMemory(level:Int) {
        super.onTrimMemory(level)
        if(level==android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && localRunner.isInitialized()) memoryScope.launch { localRunner.value.manageResources {} }
    }
    val spam by lazy { dev.kyzenn.nivoryn.detection.SpamClassifier.load(this) }
    val modelSetup by lazy { dev.kyzenn.nivoryn.setup.LocalAiSetup(this,llama) }
    val llama get()=localRunner.value
}
