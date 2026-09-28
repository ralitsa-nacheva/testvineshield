package com.rncoding.testvineshield.core.data.local.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATABASE_NAME =
    "vine_shield.db"

fun getVineshieldDatabaseBuilder():
        RoomDatabase.Builder<VineshieldDatabase> {

    val databasePath =
        documentDirectory()
            .let { directory ->
                "$directory/$DATABASE_NAME"
            }

    return Room.databaseBuilder(
        name = databasePath
    )
}

private fun documentDirectory(): String {

    val url =
        NSFileManager.defaultManager
            .URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null
            )

    return requireNotNull(
        url?.path
    )
}