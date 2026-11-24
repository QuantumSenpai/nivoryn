package dev.kyzenn.nivoryn.network

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.*
import dev.kyzenn.nivoryn.MainActivity
import dev.kyzenn.nivoryn.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class UsageState(val running:Boolean=false,val message:String="Inactive · start observation explicitly",val samples:List<TrafficSample> = emptyList())
object UsageObservation {
    val state=MutableStateFlow(UsageState())
}
class UsageMonitorService:Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private var observation:Job?=null
    private var destroyed=false
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="stop") { stopSelf(); return START_NOT_STICKY }
        if(observation?.isActive==true) return START_NOT_STICKY
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("usage","Network usage observation",NotificationManager.IMPORTANCE_LOW))
        val stop=PendingIntent.getService(this,0,Intent(this,UsageMonitorService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).putExtra("workspace","network"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,"usage").setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nivoryn · observing network usage").setContentText("Byte totals only. No VPN, payload capture or blocking.")
            .setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
        if(Build.VERSION.SDK_INT>=34) startForeground(42,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(42,notification)
        UsageObservation.state.value=UsageState(true,"Observing live device totals · no packet inspection")
        observation=scope.launch {
            val window=TrafficWindow()
            try {
                while(isActive) {
                    val connectivity=getSystemService(ConnectivityManager::class.java)
                    val capabilities=connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                    val transport=when {
                        capabilities==null->"No active network"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)->"VPN active · aggregate counts may overlap"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Mobile"
                        else->"Other network"
                    }
                    val samples=window.record(SystemClock.elapsedRealtime(),TrafficStats.getTotalRxBytes(),TrafficStats.getTotalTxBytes(),transport)
                    synchronized(UsageObservation) { if(!destroyed) UsageObservation.state.value=UsageState(true,"Observing · $transport",samples) }
                    delay(5000)
                }
            } catch(e:CancellationException) { throw e }
            catch(_:Exception) { UsageObservation.state.value=UsageState(false,"Observation failed. Device counters may be unavailable."); stopSelf() }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        scope.cancel()
        synchronized(UsageObservation) {
            destroyed=true
            if(UsageObservation.state.value.running) UsageObservation.state.value=UsageObservation.state.value.copy(running=false,message="Stopped · samples will clear when the process ends")
        }
        super.onDestroy()
    }
}
