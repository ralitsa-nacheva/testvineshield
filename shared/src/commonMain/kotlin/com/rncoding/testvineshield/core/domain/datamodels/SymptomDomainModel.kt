package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

data class SymptomDomainModel
(val symptomId: Long,
 val code: String?,
 val name: String,
 val plantPart: String,
 val phenologicalStage: PhenologicalStage,
 val notes: String
)
