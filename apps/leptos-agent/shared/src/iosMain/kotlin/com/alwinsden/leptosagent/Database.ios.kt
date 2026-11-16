package com.alwinsden.leptosagent

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/local_database.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
    )
}

object DatabaseProvider {
    val database : AppDatabase by lazy {
        getRoomDatabase(
            getDatabaseBuilder(),
            debugModeMigrationErase = BuildKonfig.IS_XCODE_DEBUG_BUILD
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
