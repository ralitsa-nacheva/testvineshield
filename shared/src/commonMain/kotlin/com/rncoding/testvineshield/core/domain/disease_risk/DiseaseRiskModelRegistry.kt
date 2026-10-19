package com.rncoding.testvineshield.core.domain.disease_risk

class DiseaseRiskModelRegistry(
    models: List<DiseaseRiskModel>
) {

    private val modelsByType: Map<DiseaseRiskModelType, DiseaseRiskModel>

    init {
        val duplicates = models
            .groupBy { it.modelType }
            .filterValues { it.size > 1 }
            .keys

        require(duplicates.isEmpty()) {
            "Duplicate disease-risk model types: $duplicates"
        }

        modelsByType = models.associateBy { it.modelType }
    }

    fun getModel(
        modelType: DiseaseRiskModelType
    ): DiseaseRiskModel? {
        return modelsByType[modelType]
    }

    fun requireModel(
        modelType: DiseaseRiskModelType
    ): DiseaseRiskModel {
        return requireNotNull(modelsByType[modelType]) {
            "No disease-risk model registered for $modelType"
        }
    }

    fun getAllModels(): List<DiseaseRiskModel> {
        return modelsByType.values.toList()
    }

    fun getRegisteredTypes(): Set<DiseaseRiskModelType> {
        return modelsByType.keys
    }
}