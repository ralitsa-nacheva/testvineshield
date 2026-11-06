package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import kotlinx.datetime.LocalDate

data class VineyardDomainModel(
    val vineyardId: Long,
    val userId: Long,
    val name: String,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val elevation: Double?,
    val sortOrder: Int,
    val createdAt: LocalDate,
    val updatedAt: LocalDate,
    val sizeUnit: AreaUnit? = null
)