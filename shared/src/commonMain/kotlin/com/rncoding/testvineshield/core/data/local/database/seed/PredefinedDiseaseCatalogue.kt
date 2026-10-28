
package com.rncoding.testvineshield.core.data.local.database.seed

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes

object PredefinedDiseaseCatalogue {

    val diseases: List<DiseaseEntity>
        get() = listOf(
            DiseaseEntity(
                diseaseId = 0L,
                code = DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW,
                name = "Grape powdery mildew",
                description = "Fungal disease of grapevines affecting leaves, shoots, and berries.",
                vectors = "Airborne fungal spores",
                factors = "Favorable temperatures and susceptible vine growth",
                treatmentSuggestion = "Monitor susceptible growth, improve canopy management, and follow locally approved disease-control recommendations.",
                isPredefined = true
            ),
            DiseaseEntity(
                diseaseId = 0L,
                code = DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW,
                name = "Grape downy mildew",
                description = "Disease affecting grapevine leaves and fruit, particularly under wet conditions.",
                vectors = "Spores dispersed by wind and water",
                factors = "Rainfall, leaf wetness, and favorable temperatures",
                treatmentSuggestion = "Monitor infection conditions, manage canopy humidity, and follow locally approved disease-control recommendations.",
                isPredefined = true
            ),
            DiseaseEntity(
                diseaseId = 0L,
                code = DiseaseCatalogueCodes.GRAPE_BOTRYTIS,
                name = "Botrytis bunch rot",
                description = "Fungal disease that can cause decay of grape clusters.",
                vectors = "Fungal spores and infected plant material",
                factors = "Humidity, moisture, damaged berries, and dense clusters",
                treatmentSuggestion = "Improve airflow, minimize berry damage, and follow locally approved disease-control recommendations.",
                isPredefined = true
            )
        )
}
