package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: Int = 0, // always single row, only one logged in user
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long
)