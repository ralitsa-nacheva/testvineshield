package com.rncoding.testvineshield.core.presentation.disease_catalogue

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes

internal data class SymptomReference(val plantPart: String, val description: String)

// Summarized from the UC IPM grape guidelines. These entries are educational
// copy and do not represent saved observations or symptom table records.
internal fun builtInSymptomReference(diseaseCode: String?): List<SymptomReference> =
    when (diseaseCode) {
        DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW -> listOf(
            SymptomReference("Leaves", "Pale spots can appear on the upper surface. Check the underside for fine white growth that can become powdery."),
            SymptomReference("Berries and cluster stems", "White powdery patches may spread over berries or cluster stems. Older berry patches can leave dark, web-like scars.")
        )
        DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW -> listOf(
            SymptomReference("Leaves", "Look for yellow patches with an oily appearance. Older lesions can be angular and stop at the veins."),
            SymptomReference("Leaf undersides", "Check the area beneath a lesion for fine white growth.")
        )
        DiseaseCatalogueCodes.GRAPE_BOTRYTIS -> listOf(
            SymptomReference("Ripening berries", "Affected berries may turn brown or reddish and develop a gray, velvety coating."),
            SymptomReference("Shoots", "Soft brown patches can develop before ripening. Affected shoots may wilt or break.")
        )
        else -> emptyList()
    }
