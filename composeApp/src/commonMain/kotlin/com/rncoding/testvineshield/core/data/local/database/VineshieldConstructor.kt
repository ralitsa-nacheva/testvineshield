package com.rncoding.testvineshield.core.data.local.database

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object VineshieldConstructor: RoomDatabaseConstructor<VineshieldDatabase> {
    override fun initialize(): VineshieldDatabase
}