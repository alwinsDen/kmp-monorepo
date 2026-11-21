package com.alwinsden.eightparams.health

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun getHealthDatabaseBuilder(context: Context): RoomDatabase.Builder<HealthDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("health_entries.db")
    return Room.databaseBuilder<HealthDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
