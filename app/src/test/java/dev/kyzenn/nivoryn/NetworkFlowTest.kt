package dev.kyzenn.nivoryn
import dev.kyzenn.nivoryn.network.PcapInspector
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
class NetworkFlowTest {
    @Test fun malformedCaptureFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) { PcapInspector.inspect(ByteArray(24)) }
        assertThrows(IllegalArgumentException::class.java) { PcapInspector.inspect(ByteArray(3)) }
    }
    @Test fun safeSynReplayExposesOnlyObservableFeatures() {
        val p = ByteBuffer.allocate(24 + 40 * 56).order(ByteOrder.LITTLE_ENDIAN)
        p.putInt(0xa1b2c3d4.toInt()).putShort(2).putShort(4).putInt(0).putInt(0).putInt(65535).putInt(101)
        repeat(40) { n ->
            p.putInt(1).putInt(n).putInt(40).putInt(40)
            val ip = ByteArray(40); ip[0] = 0x45; ip[2] = 0; ip[3] = 40; ip[9] = 6
            ip[12] = 10; ip[15] = 1; ip[16] = 10; ip[19] = 2
            ip[20] = 4; ip[21] = 0; ip[22] = 0; ip[23] = n.toByte(); ip[32] = 0x50; ip[33] = 2
            p.put(ip)
        }
        val report = PcapInspector.inspect(p.array())
        assertEquals(40, report.packets)
        assertEquals(40, report.flows.size)
        assertEquals("TCP", report.flows.first().key.protocol)
        assertEquals(40L, report.flows.first().ipBytes)
        assertTrue(report.findings.any { it.contains("destination ports") })
        assertFalse(report.toString().contains("malware confirmed"))
    }
}
