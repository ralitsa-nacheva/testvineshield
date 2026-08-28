package com.rncoding.testvineshield.core.domain.datamodels

import kotlinx.datetime.LocalDate

data class VineyardDomainModel(
    val vineyardId: Long,
    val userId: Long,
    val name: String,
    val locationId: Int,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val elevation: Int,
    val createdAt: LocalDate,
    val updatedAt: LocalDate
)
