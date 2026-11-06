package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
data class VineyardInput(
    val name: String,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val elevation: Double?,
    val sizeUnit: AreaUnit? = null
)