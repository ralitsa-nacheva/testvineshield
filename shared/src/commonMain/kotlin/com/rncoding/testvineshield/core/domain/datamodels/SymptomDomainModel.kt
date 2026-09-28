package com.rncoding.testvineshield.core.domain.datamodels

data class SymptomDomainModel
(    val symptomId: Long,
     val name: String,
     val plantPart: String,
     val phenologicalStage: String,
     val observedAt: Long,
     val notes: String)
