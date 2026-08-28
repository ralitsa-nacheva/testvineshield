package com.rncoding.testvineshield.vineyard_details.domain

import androidx.room.ForeignKey
import kotlinx.datetime.format.DateTimeFormat

data class VineyardProduce(
    val id: Int,
    val foreignKey: ForeignKey, // vineyard id
    val foreignKey: ForeignKey, // planted vine id
    val produce_date: DateTimeFormat,
    val produce: Int //kg?
)
