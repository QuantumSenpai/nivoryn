package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.network.TrafficWindow
import org.junit.Assert.*
import org.junit.Test

class TrafficWindowTest {
    @Test fun computesDeltasAndKeepsOnlySixtySamples() {
        val window=TrafficWindow()
        assertTrue(window.record(0,100,200,"Wi-Fi").isEmpty())
        val first=window.record(5000,1100,1200,"Mobile").single()
        assertEquals(1000L,first.received)
        assertEquals(400L,first.bytesPerSecond)
        repeat(100) { window.record(10000L+it*5000,2000L+it*1000,3000L+it*1000,"Wi-Fi") }
        assertEquals(60,window.record(600000,200000,300000,"VPN").size)
    }
    @Test fun counterResetNeverBecomesAnInventedLargeTransfer() {
        val window=TrafficWindow()
        window.record(1000,1000000,1000000,"Wi-Fi")
        assertTrue(window.record(2000,0,0,"No network").isEmpty())
        assertEquals(200L,window.record(3000,100,100,"Mobile").single().bytesPerSecond)
        assertThrows(IllegalArgumentException::class.java) { window.record(4000,-1,200,"Unsupported") }
    }
}
