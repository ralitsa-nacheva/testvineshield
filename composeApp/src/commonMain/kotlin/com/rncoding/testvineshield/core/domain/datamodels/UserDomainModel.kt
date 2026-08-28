package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo

data class UserDomainModel(
    val userId: Long,
    val userName: String,
    val passHash: String,
    val passSalt: String
)
