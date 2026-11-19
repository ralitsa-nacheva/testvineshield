package com.rncoding.testvineshield.core.presentation.disease_catalogue

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.StringResource

internal data class SymptomReference(val key: String, val plantPart: StringResource, val description: StringResource)

// Summarized from the UC IPM grape guidelines. These entries are educational
// copy and do not represent saved observations or symptom table records.
internal fun builtInSymptomReference(diseaseCode: String?): List<SymptomReference> =
    when (diseaseCode) {
        DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW -> listOf(
            SymptomReference("powdery_leaf", Res.string.catalogue_powdery_leaf_part, Res.string.catalogue_powdery_leaf_description),
            SymptomReference("powdery_cluster", Res.string.catalogue_powdery_cluster_part, Res.string.catalogue_powdery_cluster_description)
        )
        DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW -> listOf(
            SymptomReference("downy_leaf", Res.string.catalogue_downy_leaf_part, Res.string.catalogue_downy_leaf_description),
            SymptomReference("downy_underside", Res.string.catalogue_downy_underside_part, Res.string.catalogue_downy_underside_description)
        )
        DiseaseCatalogueCodes.GRAPE_BOTRYTIS -> listOf(
            SymptomReference("botrytis_berry", Res.string.catalogue_botrytis_berry_part, Res.string.catalogue_botrytis_berry_description),
            SymptomReference("botrytis_shoot", Res.string.catalogue_botrytis_shoot_part, Res.string.catalogue_botrytis_shoot_description)
        )
        else -> emptyList()
    }
