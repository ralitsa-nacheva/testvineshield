package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "user",
        indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    @ColumnInfo(name = "user_email")
    val userEmail: String,
    @ColumnInfo(name = "pass_hash")
    val passHash: String,
    @ColumnInfo(name = "pass_salt")
    val passSalt: String,
   // @ColumnInfo(name = "created_at")
    //val createdAt: Long

    )