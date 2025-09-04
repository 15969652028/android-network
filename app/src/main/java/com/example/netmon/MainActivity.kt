package com.example.netmon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.netmon.databinding.ActivityMainBinding
import com.example.netmon.net.DeviceIds

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val uiHandler = Handler(Looper.getMainLooper())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> startService(Intent(this, NetworkMonitorService::class.java)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val prefs = getSharedPreferences("netmon", MODE_PRIVATE)

        val env = prefs.getString("env", "prod")
        b.rbProd.isChecked = env == "prod"
        b.rbStage.isChecked = env == "stage"
        b.etServer.setText(prefs.getString("serverOverride", ""))
        b.etProbe.setText(prefs.getString("probe", "https://www.google.com/generate_204"))
        b.etInterval.setText((prefs.getInt("intervalSec", 5)).toString())
        b.cbCompat.isChecked = prefs.getBoolean("compatEnhanced", true)

        val prodBase = "https://fans.91haoka.cn"
        val stageBase = "https://fans.gantanhaokeji.top"

        fun clampInterval(sec: Int): Int {
            val min = 3
            val max = 3600
            var out = sec
            if (out < min) { out = min; Toast.makeText(this, "已将周期下限调整为 ${min} 秒", Toast.LENGTH_SHORT).show() }
            if (out > max) { out = max; Toast.makeText(this, "已将周期上限调整为 ${max} 秒", Toast.LENGTH_SHORT).show() }
            return out
        }

        fun savePrefs() {
            val selectedEnv = if (b.rbProd.isChecked) "prod" else "stage"
            val override = b.etServer.text.toString().trim()
            val inputInterval = b.etInterval.text.toString().toIntOrNull() ?: 5
            val interval = clampInterval(inputInterval)
            prefs.edit()
                .putString("env", selectedEnv)
                .putString("serverOverride", override)
                .putString("probe", b.etProbe.text.toString())
                .putInt("intervalSec", interval)
                .putBoolean("compatEnhanced", b.cbCompat.isChecked)
                .putString("server", if (override.isNotEmpty()) override else if (selectedEnv == "prod") prodBase else stageBase)
                .apply()
        }

        val deviceId = DeviceIds.getOrCache(this)
        b.tvDevice.text = "设备ID/机型：$deviceId / ${android.os.Build.BRAND} ${android.os.Build.MODEL}"

        b.btnStart.setOnClickListener {
            savePrefs()
            val needPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            if (needPermissions.isNotEmpty()) {
                permissionLauncher.launch(needPermissions.toTypedArray())
            } else {
                startService(Intent(this, NetworkMonitorService::class.java))
            }
        }
        b.btnStop.setOnClickListener { stopService(Intent(this, NetworkMonitorService::class.java)) }

        fun tick() {
            val server = prefs.getString("server", "-")
            val interval = prefs.getInt("intervalSec", 5)
            val latency = prefs.getLong("last_latency", -1L)
            val lastUpload = prefs.getString("last_upload_time", "-")
            val lastEvent = prefs.getString("last_event", "-")
            b.tvServer.text = "目标服务：$server"
            b.tvInterval.text = "推送周期：${interval}s"
            b.tvLatency.text = "最近延迟(ms)：${if (latency >= 0) latency else "-"}"
            b.tvLastUpload.text = "最后上报时间：$lastUpload"
            b.tvLastEvent.text = "最后事件：$lastEvent"
            uiHandler.postDelayed({ tick() }, 1000)
        }
        tick()
    }
}
