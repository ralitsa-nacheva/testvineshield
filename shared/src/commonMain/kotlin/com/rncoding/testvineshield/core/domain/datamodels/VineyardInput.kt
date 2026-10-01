package com.rncoding.testvineshield.core.domain.datamodels

data class VineyardInput(
    val name: String,
    val size: Double,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val elevation: Int
)