package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class BlockStageObservation(val observedOn: LocalDate, val stage: PhenologicalStage)
