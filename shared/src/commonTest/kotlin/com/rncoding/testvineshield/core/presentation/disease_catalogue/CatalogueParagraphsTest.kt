package com.rncoding.testvineshield.core.presentation.disease_catalogue

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import kotlin.test.*

class CatalogueParagraphsTest {
    private val builtIn = DiseaseDomainModel(
        diseaseId = 7L, code = DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW,
        name = "Grape powdery mildew",
        description = "Fungal disease of grapevines affecting leaves, shoots, and berries.",
        vectors = "Airborne fungal spores",
        factors = "Favorable temperatures and susceptible vine growth",
        treatmentSuggestion = "Monitor susceptible growth, improve canopy management, and follow locally approved disease-control recommendations.",
        isPredefined = true
    )

    @Test fun supportedBuiltInCopyHasTranslations() {
        CatalogueParagraph.entries.forEach { assertNotNull(catalogueParagraphResource(builtIn, it)) }
    }

    @Test fun customOrUnknownRecordsKeepTheirStoredText() {
        for (record in listOf(builtIn.copy(isPredefined = false), builtIn.copy(code = null), builtIn.copy(code = "future_disease"))) {
            CatalogueParagraph.entries.forEach { assertNull(catalogueParagraphResource(record, it)) }
        }
    }

    @Test fun changedParagraphFallsBackWithoutDisablingOtherTranslations() {
        val changed = builtIn.copy(description = "Locally amended description")
        assertNull(catalogueParagraphResource(changed, CatalogueParagraph.DESCRIPTION))
        assertNotNull(catalogueParagraphResource(changed, CatalogueParagraph.SPREAD))
        assertNull(catalogueParagraphResource(builtIn.copy(vectors = ""), CatalogueParagraph.SPREAD))
    }
}
