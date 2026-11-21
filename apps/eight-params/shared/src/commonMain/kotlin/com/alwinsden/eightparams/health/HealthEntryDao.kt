package com.alwinsden.eightparams.health

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Insert
    suspend fun insert(entry: HealthEntryEntity): Long

    @Query("DELETE FROM health_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM health_entries WHERE type = :type ORDER BY timestampMillis ASC")
    fun entriesByType(type: String): Flow<List<HealthEntryEntity>>

    @Query("SELECT * FROM health_entries ORDER BY timestampMillis ASC")
    fun allEntries(): Flow<List<HealthEntryEntity>>
}
