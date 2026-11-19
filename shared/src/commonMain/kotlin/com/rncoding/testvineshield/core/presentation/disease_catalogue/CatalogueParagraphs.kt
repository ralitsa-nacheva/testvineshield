package com.rncoding.testvineshield.core.presentation.disease_catalogue

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class CatalogueParagraph { DESCRIPTION, SPREAD, CONDITIONS, MANAGEMENT }

private data class ParagraphTranslation(val original: String, val resource: StringResource)

// Exact English seed text identifies this supported copy revision. Changed or custom
// paragraphs fall back to stored text until their translation is explicitly reviewed.
private val translations = mapOf(
    DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW to mapOf(
        CatalogueParagraph.DESCRIPTION to ParagraphTranslation("Fungal disease of grapevines affecting leaves, shoots, and berries.", Res.string.catalogue_copy_1_description),
        CatalogueParagraph.SPREAD to ParagraphTranslation("Airborne fungal spores", Res.string.catalogue_copy_1_vectors),
        CatalogueParagraph.CONDITIONS to ParagraphTranslation("Favorable temperatures and susceptible vine growth", Res.string.catalogue_copy_1_factors),
        CatalogueParagraph.MANAGEMENT to ParagraphTranslation("Monitor susceptible growth, improve canopy management, and follow locally approved disease-control recommendations.", Res.string.catalogue_copy_1_treatmentsuggestion),
    ),
    DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW to mapOf(
        CatalogueParagraph.DESCRIPTION to ParagraphTranslation("Disease affecting grapevine leaves and fruit, particularly under wet conditions.", Res.string.catalogue_copy_2_description),
        CatalogueParagraph.SPREAD to ParagraphTranslation("Spores dispersed by wind and water", Res.string.catalogue_copy_2_vectors),
        CatalogueParagraph.CONDITIONS to ParagraphTranslation("Rainfall, leaf wetness, and favorable temperatures", Res.string.catalogue_copy_2_factors),
        CatalogueParagraph.MANAGEMENT to ParagraphTranslation("Monitor infection conditions, manage canopy humidity, and follow locally approved disease-control recommendations.", Res.string.catalogue_copy_2_treatmentsuggestion),
    ),
    DiseaseCatalogueCodes.GRAPE_BOTRYTIS to mapOf(
        CatalogueParagraph.DESCRIPTION to ParagraphTranslation("Fungal disease that can cause decay of grape clusters.", Res.string.catalogue_copy_3_description),
        CatalogueParagraph.SPREAD to ParagraphTranslation("Fungal spores and infected plant material", Res.string.catalogue_copy_3_vectors),
        CatalogueParagraph.CONDITIONS to ParagraphTranslation("Humidity, moisture, damaged berries, and dense clusters", Res.string.catalogue_copy_3_factors),
        CatalogueParagraph.MANAGEMENT to ParagraphTranslation("Improve airflow, minimize berry damage, and follow locally approved disease-control recommendations.", Res.string.catalogue_copy_3_treatmentsuggestion),
    ),
 )

private fun DiseaseDomainModel.storedParagraph(field: CatalogueParagraph): String = when (field) {
    CatalogueParagraph.DESCRIPTION -> description
    CatalogueParagraph.SPREAD -> vectors
    CatalogueParagraph.CONDITIONS -> factors
    CatalogueParagraph.MANAGEMENT -> treatmentSuggestion
}

internal fun catalogueParagraphResource(disease: DiseaseDomainModel, field: CatalogueParagraph): StringResource? {
    if (!disease.isPredefined) return null
    val translation = translations[disease.code]?.get(field) ?: return null
    return translation.resource.takeIf { disease.storedParagraph(field) == translation.original }
}

@Composable
internal fun catalogueParagraph(disease: DiseaseDomainModel, field: CatalogueParagraph): String {
    val resource = catalogueParagraphResource(disease, field)
    return if (resource == null) disease.storedParagraph(field) else stringResource(resource)
}
