package com.rncoding.testvineshield.core.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class AndroidDatabaseFactory(
    private val context: Context
) : DatabaseFactory {

    override fun create(): RoomDatabase.Builder<VineshieldDatabase> {
        val appContext = context.applicationContext

        val dbFile = appContext.getDatabasePath(DATABASE_NAME)

        return Room.databaseBuilder<VineshieldDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }

    private companion object {
        const val DATABASE_NAME = "vineshield.db"
    }
}