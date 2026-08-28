package com.rncoding.testvineshield.vineyard_details.domain

import androidx.room.ForeignKey
import kotlinx.datetime.format.DateTimeFormat

data class PlantedVines(
    val id: Int,
    val foreignKey: ForeignKey, // id from grapevarieties
    val foreignKey: ForeignKey, // id from vineyard,
    val planted_on  : DateTimeFormat,
    val age: Int,
    val count: Int, // number of vines per vine variety

)
