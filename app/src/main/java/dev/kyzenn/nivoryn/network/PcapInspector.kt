package dev.kyzenn.nivoryn.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class FlowKey(val source: String, val sourcePort: Int, val destination: String, val destinationPort: Int, val protocol: String)
data class FlowFeatures(val key: FlowKey, val packets: Int, val ipBytes: Long, val synWithoutAck: Int)
data class NetworkReport(val packets: Int, val supported: Int, val bytes: Long, val findings: List<String>, val flows: List<FlowFeatures> = emptyList())

/** Classic PCAP only. IPv4 TCP/UDP metadata; never decodes application payload or TLS. */
object PcapInspector {
    fun inspect(data: ByteArray): NetworkReport {
        require(data.size in 24..20_000_000) { "Capture must be a classic PCAP of at most 20 MB" }
        val magic = data.take(4).map { it.toInt() and 255 }
        val little = when (magic) {
            listOf(0xd4, 0xc3, 0xb2, 0xa1), listOf(0x4d, 0x3c, 0xb2, 0xa1) -> true
            listOf(0xa1, 0xb2, 0xc3, 0xd4), listOf(0xa1, 0xb2, 0x3c, 0x4d) -> false
            else -> throw IllegalArgumentException("Unsupported PCAP magic; PCAPNG is not supported")
        }
        val b = ByteBuffer.wrap(data).order(if (little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        require(b.getShort(4).toInt() == 2 && b.getShort(6).toInt() == 4)
        val snap = b.getInt(16); val link = b.getInt(20)
        require(snap in 1..262144 && link in listOf(1, 101)) { "Only Ethernet or raw IP supported" }
        b.position(24)
        var packets = 0; var supported = 0; var bytes = 0L; var fragmented = 0
        val flows = linkedMapOf<FlowKey, FlowFeatures>()
        val ports = mutableMapOf<String, MutableSet<Int>>()
        val syns = mutableMapOf<String, Int>()
        while (b.hasRemaining()) {
            require(++packets <= 100000 && b.remaining() >= 16) { "Truncated or excessive packet records" }
            b.int; b.int
            val captured = b.int; val original = b.int
            require(captured in 0..snap && original >= captured && captured <= b.remaining()) { "Invalid record length" }
            val start = b.position(); val end = start + captured
            bytes += original.toLong()
            fun u(i: Int) = data[i].toInt() and 255
            val ip = if (link == 1) start + 14 else start
            val ethernetOk = link != 1 || (captured >= 14 && u(start + 12) == 8 && u(start + 13) == 0)
            if (ethernetOk && end - ip >= 20 && u(ip) shr 4 == 4) {
                val header = (u(ip) and 15) * 4
                val total = u(ip + 2) * 256 + u(ip + 3)
                val protocol = u(ip + 9)
                val fragment = (u(ip + 6) * 256 + u(ip + 7)) and 0x3fff
                val transportSize = if (protocol == 6) 20 else 8
                if (fragment != 0) fragmented++
                else if (header >= 20 && total >= header + transportSize && ip + header + transportSize <= end && (ip - start).toLong() + total <= original.toLong() && protocol in listOf(6, 17)) {
                    val transport = ip + header
                    val transportValid = if (protocol == 6) {
                        val tcpHeader = (u(transport + 12) shr 4) * 4
                        tcpHeader in 20..60 && total >= header + tcpHeader
                    } else {
                        val udpLength = u(transport + 4) * 256 + u(transport + 5)
                        udpLength in 8..(total - header)
                    }
                    if (!transportValid) { b.position(end); continue }
                    supported++
                    val source = (12..15).joinToString(".") { u(ip + it).toString() }
                    val port = u(ip + header + 2) * 256 + u(ip + header + 3)
                    val destination = (16..19).joinToString(".") { u(ip + it).toString() }
                    val sourcePort = u(transport) * 256 + u(transport + 1)
                    val key = FlowKey(source, sourcePort, destination, port, if (protocol == 6) "TCP" else "UDP")
                    require(key in flows || flows.size < 4096) { "Capture exceeds 4096-flow budget" }
                    val old = flows[key] ?: FlowFeatures(key, 0, 0, 0)
                    val syn = protocol == 6 && u(transport + 13) and 0x12 == 2
                    flows[key] = old.copy(packets = old.packets + 1, ipBytes = old.ipBytes + total, synWithoutAck = old.synWithoutAck + if (syn) 1 else 0)
                    ports.getOrPut(source) { mutableSetOf() }.add(port)
                    if (protocol == 6 && u(ip + header + 13) and 0x12 == 2) syns[source] = (syns[source] ?: 0) + 1
                }
            }
            b.position(end)
        }
        val findings = mutableListOf<String>()
        ports.filterValues { it.size >= 32 }.forEach { (source, values) -> findings += "$source contacted ${values.size} destination ports in this capture: possible scanning; legitimate discovery can look similar." }
        syns.filterValues { it >= 100 }.forEach { (source, count) -> findings += "$source sent $count TCP SYN packets without ACK set: review connection attempts; this alone does not prove intrusion." }
        if (fragmented > 0) findings += "$fragmented fragmented packets were excluded from flow detection."
        if (findings.isEmpty()) findings += "No configured threshold exceeded. This does not establish that the traffic is safe."
        return NetworkReport(packets, supported, bytes, findings, flows.values.toList())
    }
}
