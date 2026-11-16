package com.alwinsden.leptosagent

import androidx.room3.Database
import androidx.room3.ConstructedBy
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [ModelPreferencesEntity::class], version = 4)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): ModelPreferencesDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: androidx.room3.RoomDatabase.Builder<AppDatabase>,
    debugModeMigrationErase: Boolean = false
): AppDatabase {
    val b = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
    return (if(debugModeMigrationErase) b.fallbackToDestructiveMigration() else b).build()
}

