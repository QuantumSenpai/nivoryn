package dev.kyzenn.nivoryn.domain

/** Navigation/request identity is independent of when a coroutine observes cancellation. */
class AnalysisRequestGate {
    private var generation = 0L
    @Synchronized fun begin(): Long = ++generation
    @Synchronized fun invalidate() { generation++ }
    @Synchronized fun accepts(token: Long) = token == generation
}
