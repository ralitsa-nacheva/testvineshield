package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseOccurrenceDao
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseRiskSymptomProjection
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContext
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskHostContextProvider
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskOccurrenceEvidence
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskSymptomEvidence

class DefaultDiseaseRiskHostContextProvider(
    private val blockDao: BlockDao,
    private val diseaseOccurrenceDao: DiseaseOccurrenceDao
) : DiseaseRiskHostContextProvider {

    override suspend fun getHostContext(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long
    ): DiseaseRiskHostContext {

        val block =
            blockDao.getBlockById(
                vineyardId = vineyardId,
                blockId = blockId
            )

        if (block == null) {
            return emptyHostContext()
        }

        val occurrence =
            diseaseOccurrenceDao.getActiveOccurrenceForRisk(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId
            )

        val symptoms =
            if (occurrence != null) {
                diseaseOccurrenceDao.getRiskSymptomsForOccurrence(
                    occurrenceId = occurrence.occurrenceId
                )
            } else {
                emptyList()
            }

        return DiseaseRiskHostContext(
            phenologicalStage =
                block.phenologicalStage,

            activeOccurrence =
                occurrence?.toRiskEvidence(),

            observedSymptoms =
                symptoms.map {
                    it.toRiskEvidence()
                }
        )
    }

    private fun emptyHostContext(): DiseaseRiskHostContext {
        return DiseaseRiskHostContext(
            phenologicalStage = null,
            activeOccurrence = null,
            observedSymptoms = emptyList()
        )
    }

    private fun DiseaseOccurrenceEntity.toRiskEvidence():
            DiseaseRiskOccurrenceEvidence {

        return DiseaseRiskOccurrenceEvidence(
            occurrenceId = occurrenceId,
            diseaseId = diseaseId,
            observedAt = observedAt,
            severityPercent = severity
        )
    }

    private fun DiseaseRiskSymptomProjection.toRiskEvidence():
            DiseaseRiskSymptomEvidence {

        return DiseaseRiskSymptomEvidence(
            symptomId = symptomId,
            code = code,
            name = name,
            plantPart = plantPart,
            phenologicalStage = phenologicalStage,
            observedAt = observedAt,
            severity = severity,
            notes = notes
        )
    }
}