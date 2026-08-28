package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity


data class VineyardWithCalculations(
    @Embedded val vineyard: VineyardEntity,
    @Relation(
        parentColumn = "vineyard_id",
        entityColumn = "vineyard_id"
    )
    val calculations: List<WeatherCalculationEntity>
)
