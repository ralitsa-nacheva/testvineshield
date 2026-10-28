package com.rncoding.testvineshield.core.domain.datamodels


import kotlinx.datetime.LocalDate

data class HarvestDomainModel(
    val harvestId: Long,
    val blockId: Long, //foreign key
    val season: String,
    val harvestDate: LocalDate,
    val units: String,
    val quantity: Double,
    val brixSugarContent: Double
)
