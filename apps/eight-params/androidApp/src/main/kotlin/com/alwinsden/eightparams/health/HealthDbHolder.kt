package com.alwinsden.eightparams.health

import android.content.Context
import androidx.room3.RoomDatabase

/** Lazily-built process-wide singleton for the shared Room database. */
object HealthDbHolder {
    @Volatile
    private var instance: HealthDatabase? = null

    fun get(context: Context): HealthDatabase =
        instance ?: synchronized(this) {
            instance ?: getHealthDatabase(getHealthDatabaseBuilder(context)).also { instance = it }
        }
}
