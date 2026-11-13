package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.core.domain.disease_risk.SymptomCatalogueCodes

object ReferenceSymptomCodes {
    const val LEAF_OIL_SPOT = SymptomCatalogueCodes.DOWNY_MILDEW_OIL_SPOT
    const val POWDERY_LEAF_PATCH = "powdery_mildew_leaf_patch"
    const val GRAY_BERRY_GROWTH = "botrytis_gray_berry_growth"
}

enum class ReferenceSymptom(
    val diseaseCode: String,
    val code: String,
    val label: String,
    val plantPart: String,
    val referenceStage: PhenologicalStage
) {
    LEAF_OIL_SPOT(
        DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW, ReferenceSymptomCodes.LEAF_OIL_SPOT,
        "Leaf oil spot", "Leaf", PhenologicalStage.LEAF_DEVELOPMENT
    ),
    POWDERY_LEAF_PATCH(
        DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW, ReferenceSymptomCodes.POWDERY_LEAF_PATCH,
        "Powdery patch on leaf", "Leaf", PhenologicalStage.LEAF_DEVELOPMENT
    ),
    GRAY_BERRY_GROWTH(
        DiseaseCatalogueCodes.GRAPE_BOTRYTIS, ReferenceSymptomCodes.GRAY_BERRY_GROWTH,
        "Gray growth on berries", "Berry", PhenologicalStage.RIPENING
    );

    companion object {
        fun forDisease(code: String?): ReferenceSymptom? = entries.firstOrNull { it.diseaseCode == code }
    }
}
