package dev.kyzenn.nivoryn.reasoning

import dev.kyzenn.nivoryn.data.ModelStore
import dev.kyzenn.nivoryn.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.pytorch.executorch.extension.llm.LlmModule
import org.pytorch.executorch.extension.llm.LlmCallback
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Process-owned runner: one cached module, one job, no cloud fallback. */
class LlamaRunner(private val store: ModelStore) : AutoCloseable {
    private val mutex = Mutex()
    private val stopGate = GenerationStopGate()
    @Volatile private var module: LlmModule? = null
    private val timer = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "llama-deadline").apply { isDaemon = true } }
    suspend fun explain(result: DemoResult): DemoResult {
        if (result.assessment == Assessment.UNKNOWN) return fallback(result, "Insufficient supported evidence")
        if (!mutex.tryLock()) return fallback(result, "Runner busy")
        try {
            return withContext(Dispatchers.IO) {
                val current = module ?: store.installed()?.let { installed ->
                    LlmModule(installed.model.absolutePath, installed.tokenizer.absolutePath, 0f).also {
                        if (it.load() != 0) { it.resetNative(); error("Model load failed") }
                        module = it
                    }
                } ?: return@withContext fallback(result, "Trusted Llama resources missing")
                val job = currentCoroutineContext().job
                val generation = stopGate.begin()
                val timedOut = java.util.concurrent.atomic.AtomicBoolean(false)
                val overflow = java.util.concurrent.atomic.AtomicBoolean(false)
                val deadline = timer.schedule({ timedOut.set(true); requestStop(current, generation) }, 30, TimeUnit.SECONDS)
                // stop() is designed to be called from a different thread during native generation.
                val cancellation = timer.scheduleWithFixedDelay({ if (!job.isActive) requestStop(current, generation) }, 50, 50, TimeUnit.MILLISECONDS)
                try {
                    current.resetContext()
                    val output = StringBuilder(PromptBuilder.RESPONSE_PREFIX)
                    val code = current.generate(PromptBuilder.build(result), 1024, object : LlmCallback {
                        override fun onResult(token: String) {
                            if (output.length + token.length <= 2048) output.append(token)
                            else { overflow.set(true); requestStop(current, generation) }
                        }
                    }, false)
                    ensureActive()
                    if (timedOut.get()) return@withContext fallback(result, "Generation deadline reached")
                    if (overflow.get()) return@withContext fallback(result, "Generation output budget exceeded")
                    if (code != 0) return@withContext fallback(result, "ExecuTorch generation failed")
                    val guidance = ExplanationValidator.validate(output.toString(), result)
                        ?: return@withContext fallback(result, "Generated advice rejected")
                    result.copy(explanation = guidance.explanation, nextSteps = guidance.actions,
                        completed = result.completed + "Llama / ExecuTorch 1.2.0: generated prose; source binding ${guidance.binding.name}; approved remediation IDs",
                        explanationMode = when (guidance.binding) {
                            SourceBinding.MODEL_REFERENCES -> "Llama generated explanation · validated references"
                            SourceBinding.LOCAL_SINGLE_FINDING -> "Llama generated explanation · local evidence binding"
                            SourceBinding.NO_FINDINGS -> "Llama generated explanation · limited local checks"
                        },
                        unavailable = result.unavailable.filterNot { it == "Llama inference" }.map { if (it == "ML scam classifier and Llama inference") "ML scam classifier" else it })
                } finally {
                    stopGate.finish(generation)
                    deadline.cancel(false); cancellation.cancel(false)
                }
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { return fallback(result, "Local inference failed")
        } catch (_: LinkageError) { return fallback(result, "Runtime ABI unavailable")
        } finally { mutex.unlock() }
    }
    suspend fun <T> manageResources(action: () -> T): T = mutex.withLock {
        withContext(Dispatchers.IO) {
            releaseModule()
            action()
        }
    }
    private fun requestStop(current: LlmModule, generation: Any) = stopGate.ifActive(generation) {
        if (module === current) current.stop()
    }
    private fun releaseModule() = synchronized(stopGate) {
        stopGate.clear()
        val old = module; module = null; old?.resetNative()
    }
    private fun fallback(result: DemoResult, reason: String) = result.copy(explanationMode = "Curated fallback · $reason")
    override fun close() {
        // Call only when no generation is running; never free native state under generate().
        check(mutex.tryLock())
        try { releaseModule(); timer.shutdownNow() } finally { mutex.unlock() }
    }
}
