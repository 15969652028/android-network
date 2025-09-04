package com.example.netmon.net

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object DeviceIds {
    private const val KEY = "device_id"

    fun getOrCache(ctx: Context): String {
        val prefs = ctx.getSharedPreferences("netmon", Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY, null)
        if (!cached.isNullOrEmpty()) return cached
        val id = get(ctx)
        prefs.edit().putString(KEY, id).apply()
        return id
    }

    fun get(ctx: Context): String {
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val imei = try { tm.imei } catch (_: Throwable) { tm.deviceId }
                if (!imei.isNullOrEmpty()) return "IMEI:$imei"
            } catch (_: Exception) {}
        }
        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        return "AID:$androidId"
    }
}
