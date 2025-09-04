package com.example.netmon.data

import android.content.Context

class LogRepository private constructor(ctx: Context) {
    private val dao = AppDatabase.get(ctx).logs()

    suspend fun insert(log: NetworkLog) { dao.insert(log) }
    suspend fun getUnsent(limit: Int) = dao.getUnsent(limit)
    suspend fun markSent(ids: List<Long>) = dao.markSent(ids)

    companion object {
        @Volatile private var INSTANCE: LogRepository? = null
        fun get(ctx: Context): LogRepository = INSTANCE ?: synchronized(this) {
            INSTANCE ?: LogRepository(ctx.applicationContext).also { INSTANCE = it }
        }
    }
}
