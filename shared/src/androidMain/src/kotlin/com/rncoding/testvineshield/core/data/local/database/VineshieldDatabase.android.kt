package com.rncoding.testvineshield.core.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE_NAME =
    "vine_shield.db"

fun getVineshieldDatabaseBuilder(
    context: Context
): RoomDatabase.Builder<VineshieldDatabase> {

    val appContext =
        context.applicationContext

    val databaseFile =
        appContext.getDatabasePath(
            DATABASE_NAME
        )

    return Room.databaseBuilder(
        context = appContext,
        name = databaseFile.absolutePath
    )
}