package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.reasoning.GenerationStopGate
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerationStopGateTest {
    @Test fun delayedStopCannotStopANewGenerationOnTheSameCachedModule() {
        val gate=GenerationStopGate(); var stops=0
        val first=gate.begin()
        val delayedCallback={ gate.ifActive(first) { stops++ } }
        gate.finish(first)
        val second=gate.begin()
        delayedCallback()
        assertEquals(0,stops)
        gate.ifActive(second) { stops++ }
        gate.clear()
        gate.ifActive(second) { stops++ }
        assertEquals(1,stops)
    }
}
