package com.example.netmon.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object NetUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /** 详细 HTTP 探测：返回 (成功, 延迟ms, HTTP code, 错误消息) */
    fun httpProbeDetailed(url: String): Quad<Boolean, Long?, Int?, String?> {
        val req = Request.Builder().url(url).build()
        val start = System.nanoTime()
        return try {
            client.newCall(req).execute().use { resp ->
                val latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                Quad(resp.isSuccessful, latency, resp.code, null)
            }
        } catch (e: Exception) {
            Quad(false, null, null, e.message)
        }
    }

    /** 生成带时区的 ISO8601 字符串 */
    fun nowIso8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    data class Quad<A,B,C,D>(val first: A, val second: B, val third: C, val fourth: D)
}
