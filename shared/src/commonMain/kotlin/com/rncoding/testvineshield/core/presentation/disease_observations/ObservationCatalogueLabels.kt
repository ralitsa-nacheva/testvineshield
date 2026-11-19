package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.datamodels.ReferenceSymptom
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Presentation labels only: database names and stable codes are never rewritten. */
@Composable
internal fun builtInDiseaseName(code: String?, fallback: String): String = when (code) {
    DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW -> stringResource(Res.string.disease_name_downy)
    DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW -> stringResource(Res.string.disease_name_powdery)
    DiseaseCatalogueCodes.GRAPE_BOTRYTIS -> stringResource(Res.string.disease_name_botrytis)
    else -> fallback
}

@Composable
internal fun referenceSymptomName(symptom: ReferenceSymptom): String = stringResource(when (symptom) {
    ReferenceSymptom.LEAF_OIL_SPOT -> Res.string.symptom_leaf_oil_spot
    ReferenceSymptom.POWDERY_LEAF_PATCH -> Res.string.symptom_powdery_leaf
    ReferenceSymptom.GRAY_BERRY_GROWTH -> Res.string.symptom_gray_berries
    ReferenceSymptom.POWDERY_CLUSTER_PATCH -> Res.string.symptom_powdery_cluster
    ReferenceSymptom.DOWNY_LEAF_UNDERSIDE -> Res.string.symptom_downy_underside
    ReferenceSymptom.BOTRYTIS_SHOOT_PATCH -> Res.string.symptom_brown_shoot
})
