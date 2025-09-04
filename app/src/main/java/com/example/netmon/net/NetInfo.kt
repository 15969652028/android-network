package com.example.netmon.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.*

data class NetSnapshot(
    val netType: String? = null, // WIFI / CELL / OTHER
    val ssid: String? = null,
    val wifiRssi: Int? = null,
    val carrier: String? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val signalDbm: Int? = null
)

object NetInfo {
    @SuppressLint("MissingPermission")
    fun collect(ctx: Context, compatEnhanced: Boolean = true): NetSnapshot {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELL"
            else -> "OTHER"
        }

        var ssid: String? = null
        var wifiRssi: Int? = null
        try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            if (info != null && info.networkId != -1) {
                ssid = info.ssid?.trim('"')
                wifiRssi = info.rssi
            }
        } catch (_: Exception) {}

        var carrier: String? = null
        var rsrp: Int? = null
        var rsrq: Int? = null
        var sinr: Int? = null
        var dbm: Int? = null
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            carrier = tm.networkOperatorName
            val cells = tm.allCellInfo
            if (!cells.isNullOrEmpty()) {
                val primary = cells.maxByOrNull { it.cellSignalStrength.dbm } ?: cells.first()
                val strength = primary.cellSignalStrength
                dbm = strength.dbm

                when (primary) {
                    is CellInfoNr -> if (Build.VERSION.SDK_INT >= 29) {
                        val nr = primary.cellSignalStrength as CellSignalStrengthNr
                        try { rsrp = nr.ssRsrp } catch (_: Throwable) {}
                        try { rsrq = nr.ssRsrq } catch (_: Throwable) {}
                        try { sinr = nr.ssSinr } catch (_: Throwable) {}
                    }
                    is CellInfoLte -> {
                        val lte = primary.cellSignalStrength as CellSignalStrengthLte
                        try { rsrp = if (Build.VERSION.SDK_INT >= 29) lte.rsrp else null } catch (_: Throwable) {}
                        try { rsrq = if (Build.VERSION.SDK_INT >= 29) lte.rsrq else null } catch (_: Throwable) {}
                    }
                }
                if (compatEnhanced) try {
                    if (rsrp == null) rsrp = reflectInt(strength, "rsrp")
                    if (rsrq == null) rsrq = reflectInt(strength, "rsrq")
                    if (sinr == null) sinr = reflectInt(strength, "sinr")
                } catch (_: Throwable) {}
            }
        } catch (_: Exception) {}

        return NetSnapshot(netType, ssid, wifiRssi, carrier, rsrp, rsrq, sinr, dbm)
    }

    private fun reflectInt(any: Any, field: String): Int? = try {
        val m = any.javaClass.methods.firstOrNull { it.name.equals(field, true) && it.parameterTypes.isEmpty() }
        (m?.invoke(any) as? Int)
    } catch (_: Throwable) { null }
}
