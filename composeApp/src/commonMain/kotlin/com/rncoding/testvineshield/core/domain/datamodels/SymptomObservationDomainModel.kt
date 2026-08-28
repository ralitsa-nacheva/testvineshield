package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class SymptomObservationDomainModel
(    val symptomId: Long,
     val name: String,
     val plantPart: String,
     val phenologicalStage: String,
     val observedAt: Long,
     val notes: String)
