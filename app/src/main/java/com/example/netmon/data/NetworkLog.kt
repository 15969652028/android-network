package com.example.netmon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_logs")
data class NetworkLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isoTime: String,          // ISO8601（含时区）
    val event: String,            // connect / disconnect / switch / unstable / offline / error / heartbeat
    val netType: String? = null,  // WIFI / CELL / OTHER
    val latencyMs: Long? = null,
    val level: String = "INFO",
    val message: String? = null,
    // 设备标识
    val deviceId: String,
    val brand: String,
    val model: String,
    // Wi-Fi
    val ssid: String? = null,
    val wifiRssi: Int? = null,
    // 蜂窝
    val carrier: String? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val signalDbm: Int? = null,
    val sent: Boolean = false
) {
    companion object {
        fun event(
            event: String,
            netType: String? = null,
            latencyMs: Long? = null,
            level: String = "INFO",
            message: String? = null,
            deviceId: String,
            brand: String,
            model: String,
            ssid: String? = null,
            wifiRssi: Int? = null,
            carrier: String? = null,
            rsrp: Int? = null,
            rsrq: Int? = null,
            sinr: Int? = null,
            signalDbm: Int? = null,
            isoTime: String
        ) = NetworkLog(
            event = event, netType = netType, latencyMs = latencyMs, level = level, message = message,
            deviceId = deviceId, brand = brand, model = model,
            ssid = ssid, wifiRssi = wifiRssi, carrier = carrier, rsrp = rsrp, rsrq = rsrq, sinr = sinr,
            signalDbm = signalDbm, isoTime = isoTime
        )
    }
}
