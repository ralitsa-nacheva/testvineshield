package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
import com.rncoding.testvineshield.core.domain.error.VineyardValidationReason
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedVineyardValidationError(error: VineyardValidationError): String {
    val resource = when (error.reason) {
        VineyardValidationReason.NameRequired -> Res.string.vineyard_validation_namerequired
        VineyardValidationReason.NameTooLong -> Res.string.vineyard_validation_nametoolong
        VineyardValidationReason.SizePositive -> Res.string.vineyard_validation_sizepositive
        VineyardValidationReason.SizeNumber -> Res.string.vineyard_validation_sizenumber
        VineyardValidationReason.CountryRequired -> Res.string.vineyard_validation_countryrequired
        VineyardValidationReason.CityRequired -> Res.string.vineyard_validation_cityrequired
        VineyardValidationReason.LatitudeRange -> Res.string.vineyard_validation_latituderange
        VineyardValidationReason.LongitudeRange -> Res.string.vineyard_validation_longituderange
        VineyardValidationReason.TimeZoneRequired -> Res.string.vineyard_validation_timezonerequired
        VineyardValidationReason.ElevationRange -> Res.string.vineyard_validation_elevationrange
        VineyardValidationReason.LatitudeNumber -> Res.string.vineyard_validation_latitudenumber
        VineyardValidationReason.LongitudeNumber -> Res.string.vineyard_validation_longitudenumber
        VineyardValidationReason.ElevationNumber -> Res.string.vineyard_validation_elevationnumber
        VineyardValidationReason.TimeZoneInvalid -> Res.string.vineyard_validation_timezoneinvalid
        null -> null
    }
    return if (resource == null) error.userMessage else stringResource(resource)
}
