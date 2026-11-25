package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedDiseaseError(error: DiseaseError): String = when (error) {
    DiseaseError.NotFound -> stringResource(Res.string.disease_error_notfound)
    DiseaseError.PredefinedDisease -> stringResource(Res.string.disease_error_predefineddisease)
    DiseaseError.DiseaseInUse -> stringResource(Res.string.disease_error_diseaseinuse)
    is DiseaseError.InvalidInput -> error.reason?.let { reason -> stringResource(when (reason) {
        DiseaseValidationReason.Name -> Res.string.disease_error_name
        DiseaseValidationReason.Code -> Res.string.disease_error_code
        DiseaseValidationReason.ReservedCode -> Res.string.disease_error_reservedcode
        DiseaseValidationReason.NewIdentity -> Res.string.disease_error_newidentity
        DiseaseValidationReason.Scope -> Res.string.disease_error_scope
        DiseaseValidationReason.ScopeUnavailable -> Res.string.disease_error_scopeunavailable
        DiseaseValidationReason.ObservationAfterResolution -> Res.string.disease_error_observationafterresolution
        DiseaseValidationReason.ObservationChanged -> Res.string.disease_error_observationchanged
        DiseaseValidationReason.DeleteChanged -> Res.string.disease_error_deletechanged
        DiseaseValidationReason.SymptomUnavailable -> Res.string.disease_error_symptomunavailable
        DiseaseValidationReason.SymptomAfterResolution -> Res.string.disease_error_symptomafterresolution
        DiseaseValidationReason.Notes -> Res.string.disease_error_notes
        DiseaseValidationReason.SymptomChanged -> Res.string.disease_error_symptomchanged
        DiseaseValidationReason.SymptomMissing -> Res.string.disease_error_symptommissing
        DiseaseValidationReason.Identity -> Res.string.disease_error_identity
        DiseaseValidationReason.LinkZone -> Res.string.disease_error_linkzone
        DiseaseValidationReason.ActivityFuture -> Res.string.disease_error_activityfuture
        DiseaseValidationReason.ActivityChanged -> Res.string.disease_error_activitychanged
        DiseaseValidationReason.LinkMissing -> Res.string.disease_error_linkmissing
        DiseaseValidationReason.Status -> Res.string.disease_error_status
        DiseaseValidationReason.ResolutionZone -> Res.string.disease_error_resolutionzone
        DiseaseValidationReason.ResolutionBeforeObservation -> Res.string.disease_error_resolutionbeforeobservation
        DiseaseValidationReason.Severity -> Res.string.disease_error_severity
        DiseaseValidationReason.ObservationZone -> Res.string.disease_error_observationzone
        DiseaseValidationReason.ObservationFuture -> Res.string.disease_error_observationfuture
    }) } ?: stringResource(Res.string.error_review_input)
}
