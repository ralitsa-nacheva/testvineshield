package com.rncoding.testvineshield.geo_location.locals

import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<VineshieldDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(VineshieldDatabase.DB_NAME)

        return Room.databaseBuilder(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}