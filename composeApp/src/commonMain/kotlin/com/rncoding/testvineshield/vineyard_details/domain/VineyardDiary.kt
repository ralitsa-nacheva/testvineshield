package com.rncoding.testvineshield.vineyard_details.domain

import androidx.room.ForeignKey
import kotlinx.datetime.format.DateTimeFormat

data class VineyardDiary(
    val id: Int,
    val foreignKey: ForeignKey, // id from phenology for the phenological state
    val foreignKey: ForeignKey, // id from plantedvines
    val task_date: DateTimeFormat, //date of the performed task
    val foreignKey: ForeignKey, // id from agrotasks for the performed task
)
