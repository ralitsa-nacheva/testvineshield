package com.rncoding.testvineshield.core.presentation.vineyard_details

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedStage(stage: PhenologicalStage?): String = stringResource(
    when (stage) {
        null -> Res.string.unknown_value
        PhenologicalStage.DORMANCY -> Res.string.stage_dormancy
        PhenologicalStage.BUD_SWELL -> Res.string.stage_bud_swell
        PhenologicalStage.BUD_BURST -> Res.string.stage_bud_burst
        PhenologicalStage.LEAF_DEVELOPMENT -> Res.string.stage_leaf_development
        PhenologicalStage.INFLORESCENCE_DEVELOPMENT -> Res.string.stage_inflorescence_development
        PhenologicalStage.FLOWERING -> Res.string.stage_flowering
        PhenologicalStage.FRUIT_SET -> Res.string.stage_fruit_set
        PhenologicalStage.BERRY_DEVELOPMENT -> Res.string.stage_berry_development
        PhenologicalStage.VERAISON -> Res.string.stage_veraison
        PhenologicalStage.RIPENING -> Res.string.stage_ripening
        PhenologicalStage.HARVEST -> Res.string.stage_harvest
        PhenologicalStage.POST_HARVEST -> Res.string.stage_post_harvest
        PhenologicalStage.LEAF_FALL -> Res.string.stage_leaf_fall
    }
)

@Composable
internal fun localizedActivityStatus(value: ActivityStatus): String = stringResource(
    when (value) {
        ActivityStatus.PLANNED -> Res.string.status_planned
        ActivityStatus.IN_PROGRESS -> Res.string.status_progress
        ActivityStatus.COMPLETED -> Res.string.status_completed
        ActivityStatus.CANCELLED -> Res.string.status_cancelled
    }
)

@Composable
internal fun localizedOccurrenceStatus(value: DiseaseOccurrenceStatus): String = stringResource(
    when (value) {
        DiseaseOccurrenceStatus.ACTIVE -> Res.string.disease_active
        DiseaseOccurrenceStatus.MONITORING -> Res.string.disease_monitoring
        DiseaseOccurrenceStatus.RESOLVED -> Res.string.disease_resolved
    }
)

@Composable
internal fun localizedAlertSeverity(value: AlertSeverity): String = stringResource(
    when (value) {
        AlertSeverity.INFO -> Res.string.alert_info
        AlertSeverity.LOW -> Res.string.alert_low
        AlertSeverity.MEDIUM -> Res.string.alert_medium
        AlertSeverity.HIGH -> Res.string.alert_high
        AlertSeverity.CRITICAL -> Res.string.alert_critical
    }
)
