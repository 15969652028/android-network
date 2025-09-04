package com.example.netmon

import android.app.*
import android.content.*
import android.net.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.netmon.data.LogRepository
import com.example.netmon.data.NetworkLog
import com.example.netmon.net.*
import kotlinx.coroutines.*
import kotlin.math.max

class NetworkMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: LogRepository

    private var api: ApiService? = null
    private var lastServer: String? = null

    private val minIntervalSec = 3
    private val latencyWarnMs = 500L

    override fun onCreate() {
        super.onCreate()
        repo = LogRepository.get(applicationContext)
        startForeground(1, createNotification())
        registerNetworkCallbacks()
        startLoop()
    }

    private fun createNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "netmon"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(chId, "NetMon", NotificationManager.IMPORTANCE_MIN)
            )
        }
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("网络监控运行中（可配置推送周期）")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun ensureApi(): ApiService {
        val prefs = getSharedPreferences("netmon", MODE_PRIVATE)
        val server = prefs.getString("server", "http://10.0.2.2:8000")!!.trimEnd('/')
        if (server != lastServer || api == null) {
            api = ApiService.create(server)
            lastServer = server
        }
        return api!!
    }

    private fun startLoop() {
        scope.launch {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val prefs = getSharedPreferences("netmon", MODE_PRIVATE)
            val deviceId = DeviceIds.getOrCache(this@NetworkMonitorService)
            val brand = Build.BRAND
            val model = Build.MODEL
            while (isActive) {
                val intervalSec = max(minIntervalSec, prefs.getInt("intervalSec", 5))
                val delayMs = intervalSec * 1000L
                try {
                    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                    val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    val compat = prefs.getBoolean("compatEnhanced", true)
                    val info = NetInfo.collect(applicationContext, compat)

                    val now = NetUtils.nowIso8601()
                    val log = when {
                        !online -> NetworkLog.event(
                            event = "offline",
                            level = "ERROR",
                            message = "网络离线：无可用网络或被系统限制 @ $now",
                            deviceId = deviceId, brand = brand, model = model,
                            ssid = info.ssid, wifiRssi = info.wifiRssi, carrier = info.carrier,
                            rsrp = info.rsrp, rsrq = info.rsrq, sinr = info.sinr, signalDbm = info.signalDbm,
                            isoTime = now
                        )
                        else -> {
                            val url = prefs.getString("probe", "https://www.google.com/generate_204")!!
                            val (ok, latencyMs, httpCode, errorMsg) = NetUtils.httpProbeDetailed(url)
                            prefs.edit().putLong("last_latency", latencyMs ?: -1).apply()
                            when {
                                !ok -> NetworkLog.event(
                                    event = "unstable",
                                    netType = info.netType, latencyMs = latencyMs, level = "WARN",
                                    message = "HTTP 探测失败：code=${httpCode ?: -1}, error=${errorMsg ?: "unknown"} @ $now",
                                    deviceId = deviceId, brand = brand, model = model,
                                    ssid = info.ssid, wifiRssi = info.wifiRssi, carrier = info.carrier,
                                    rsrp = info.rsrp, rsrq = info.rsrq, sinr = info.sinr, signalDbm = info.signalDbm,
                                    isoTime = now
                                )
                                latencyMs != null && latencyMs > latencyWarnMs -> NetworkLog.event(
                                    event = "unstable",
                                    netType = info.netType, latencyMs = latencyMs, level = "WARN",
                                    message = "高时延：${latencyMs}ms > ${latencyWarnMs}ms @ $now",
                                    deviceId = deviceId, brand = brand, model = model,
                                    ssid = info.ssid, wifiRssi = info.wifiRssi, carrier = info.carrier,
                                    rsrp = info.rsrp, rsrq = info.rsrq, sinr = info.sinr, signalDbm = info.signalDbm,
                                    isoTime = now
                                )
                                else -> NetworkLog.event(
                                    event = "heartbeat",
                                    netType = info.netType, latencyMs = latencyMs, level = "INFO",
                                    message = "网络正常 @ $now",
                                    deviceId = deviceId, brand = brand, model = model,
                                    ssid = info.ssid, wifiRssi = info.wifiRssi, carrier = info.carrier,
                                    rsrp = info.rsrp, rsrq = info.rsrq, sinr = info.sinr, signalDbm = info.signalDbm,
                                    isoTime = now
                                )
                            }
                        }
                    }

                    repo.insert(log)

                    val unsent = repo.getUnsent(limit = 200)
                    if (unsent.isNotEmpty()) {
                        val resp = ensureApi().uploadLogs(unsent)
                        if (resp.isSuccessful) {
                            repo.markSent(unsent.map { it.id })
                            prefs.edit().putString("last_upload_time", now).apply()
                        }
                    }

                    prefs.edit().putString("last_event", "${log.event} ${log.level} ${log.message ?: ""}").apply()
                } catch (e: Exception) {
                    val now = NetUtils.nowIso8601()
                    repo.insert(
                        NetworkLog.event(
                            event = "error", level = "ERROR", message = "循环异常：${e.message} @ $now",
                            deviceId = deviceId, brand = brand, model = model, isoTime = now
                        )
                    )
                }
                delay(delayMs)
            }
        }
    }

    private fun registerNetworkCallbacks() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { repo.insert(NetworkLog.event("connect", level = "INFO", message = "网络可用", deviceId = DeviceIds.getOrCache(this@NetworkMonitorService), brand = Build.BRAND, model = Build.MODEL, isoTime = NetUtils.nowIso8601())) }
            }
            override fun onLost(network: Network) {
                scope.launch { repo.insert(NetworkLog.event("disconnect", level = "ERROR", message = "网络连接丢失", deviceId = DeviceIds.getOrCache(this@NetworkMonitorService), brand = Build.BRAND, model = Build.MODEL, isoTime = NetUtils.nowIso8601())) }
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val type = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL"
                    else -> "OTHER"
                }
                scope.launch { repo.insert(NetworkLog.event("switch", netType = type, level = "INFO", message = "网络能力变化：$type", deviceId = DeviceIds.getOrCache(this@NetworkMonitorService), brand = Build.BRAND, model = Build.MODEL, isoTime = NetUtils.nowIso8601())) }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
    override fun onBind(intent: Intent?): IBinder? = null
}
