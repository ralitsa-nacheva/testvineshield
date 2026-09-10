package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo

data class UserDomainModel(
    val userId: Long,
    val userEmail: String,
    val passHash: String,
    val passSalt: String
    // val createdAt: Long
)
