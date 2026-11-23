package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.domain.AnalysisRequestGate
import org.junit.Assert.*
import org.junit.Test
class AnalysisRequestGateTest {
    @Test fun cancelledCompletionCannotReplaceANewerCheck() {
        val gate=AnalysisRequestGate(); val first=gate.begin()
        gate.invalidate(); val next=gate.begin()
        assertFalse(gate.accepts(first)); assertTrue(gate.accepts(next))
        gate.invalidate(); assertFalse(gate.accepts(next))
    }
}
