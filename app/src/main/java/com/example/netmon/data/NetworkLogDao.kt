package com.example.netmon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NetworkLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NetworkLog): Long

    @Query("SELECT * FROM network_logs WHERE sent = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getUnsent(limit: Int): List<NetworkLog>

    @Query("UPDATE network_logs SET sent = 1 WHERE id IN (:ids)")
    suspend fun markSent(ids: List<Long>)
}
