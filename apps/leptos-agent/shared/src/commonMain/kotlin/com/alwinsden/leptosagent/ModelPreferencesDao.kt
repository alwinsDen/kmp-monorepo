package com.alwinsden.leptosagent

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelPreferencesDao {
    @Upsert
    suspend fun insert(item: ModelPreferencesEntity)

    @Query("SELECT count(*) FROM ModelPreferencesEntity")
    suspend fun count(): Int

    @Query("SELECT * FROM ModelPreferencesEntity")
    fun getAllAsFlow(): Flow<List<ModelPreferencesEntity>>
}
