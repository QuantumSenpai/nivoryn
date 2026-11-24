package dev.kyzenn.nivoryn.network

data class TrafficSample(val elapsedMs:Long,val received:Long,val sent:Long,val transport:String) {
    val bytesPerSecond:Long get()=((received.toDouble()+sent)*1000/elapsedMs.coerceAtLeast(1)).toLong()
    val note:String get()=if(bytesPerSecond>=10_000_000) "Large transfer · can be normal usage" else "Usage sample · no threat conclusion"
}
class TrafficWindow {
    private var previous:Triple<Long,Long,Long>?=null
    private val rows=ArrayDeque<TrafficSample>()
    fun record(time:Long,rx:Long,tx:Long,transport:String):List<TrafficSample> {
        require(rx>=0 && tx>=0) { "Device traffic counters are unavailable" }
        val old=previous
        if(old!=null && time>old.first && rx>=old.second && tx>=old.third) {
            rows.addLast(TrafficSample(time-old.first,rx-old.second,tx-old.third,transport))
            while(rows.size>60) rows.removeFirst()
        }
        previous=Triple(time,rx,tx)
        return rows.toList()
    }
}
