package dev.kyzenn.nivoryn.reasoning

/** Stops belong to a generation, not the cached native module reused by later requests. */
internal class GenerationStopGate {
    private var active: Any? = null
    @Synchronized fun begin(): Any = Any().also { active = it }
    @Synchronized fun finish(token: Any) { if (active === token) active = null }
    @Synchronized fun clear() { active = null }
    @Synchronized fun ifActive(token: Any, stop: () -> Unit) { if (active === token) stop() }
}
