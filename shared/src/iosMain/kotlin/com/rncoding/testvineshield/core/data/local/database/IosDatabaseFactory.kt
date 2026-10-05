package com.rncoding.testvineshield.core.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosDatabaseFactory : DatabaseFactory {

    @OptIn(ExperimentalForeignApi::class)
    override fun create(): RoomDatabase.Builder<VineshieldDatabase> {
        val documentDirectory = NSFileManager.defaultManager
            .URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )
            ?.path
            ?: error("Unable to resolve iOS document directory.")

        val dbPath = "$documentDirectory/$DATABASE_NAME"

        return Room.databaseBuilder<VineshieldDatabase>(
            name = dbPath
        )
    }

    private companion object {
        const val DATABASE_NAME = "vineshield.db"
    }
}