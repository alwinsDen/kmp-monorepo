package com.alwinsden.eightparams.health

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [HealthEntryEntity::class], version = 1)
@ConstructedBy(HealthDatabaseConstructor::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun dao(): HealthEntryDao
}

@Suppress("KotlinNoActualForExpect")
expect object HealthDatabaseConstructor : RoomDatabaseConstructor<HealthDatabase> {
    override fun initialize(): HealthDatabase
}

fun getHealthDatabase(builder: RoomDatabase.Builder<HealthDatabase>): HealthDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
