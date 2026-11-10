package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object VineyardsRoute : AppRoute

@Serializable
data class VineyardDetailsRoute(
    val vineyardId: Long
) : AppRoute

@Serializable
data object CreateVineyardRoute : AppRoute

@Serializable
data class EditVineyardRoute(
    val vineyardId: Long
) : AppRoute


@Serializable
data object ActivitiesRoute : AppRoute

@Serializable
data class ActivityDetailsRoute(
    val vineyardId: Long,
    val activityId: Long
) : AppRoute


@Serializable
data object AlertsRoute : AppRoute

@Serializable
data class DiseaseAlertDetailsRoute(
    val vineyardId: Long,
    val alertId: Long
) : AppRoute

@Serializable
data class WeatherAlertDetailsRoute(
    val vineyardId: Long,
    val alertId: Long
) : AppRoute


@Serializable
data object AccountRoute : AppRoute

@Serializable
data object SecuritySettingsRoute : AppRoute

@Serializable
data class BlocksRoute(val vineyardId: Long) : AppRoute
@Serializable
data class BlockDetailsRoute(val vineyardId: Long,val blockId: Long) : AppRoute
@Serializable
data class BlockEditorRoute(val vineyardId: Long,val blockId: Long = 0) : AppRoute


@Serializable
data class ActivityListRoute(val vineyardId: Long,val blockId: Long = 0) : AppRoute
@Serializable
data class ActivityEditorRoute(val vineyardId: Long,val blockId: Long = 0,val activityId: Long = 0,val complete: Boolean = false) : AppRoute


@Serializable
data class HarvestListRoute(val vineyardId: Long, val blockId: Long) : AppRoute
@Serializable
data class HarvestDetailsRoute(val vineyardId: Long, val blockId: Long, val harvestId: Long) : AppRoute
@Serializable
data class HarvestEditorRoute(val vineyardId: Long, val blockId: Long, val harvestId: Long = 0) : AppRoute


@Serializable
data class HarvestExportRoute(val vineyardId: Long, val blockId: Long, val season: String? = null) : AppRoute
