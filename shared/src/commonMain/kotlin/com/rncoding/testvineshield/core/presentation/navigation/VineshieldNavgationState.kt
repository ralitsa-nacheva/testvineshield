package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val vineShieldSerializersModule =
    SerializersModule {

        polymorphic(NavKey::class) {

            subclass(VineyardsRoute::class)
            subclass(VineyardDetailsRoute::class)
            subclass(CreateVineyardRoute::class)
            subclass(EditVineyardRoute::class)

            subclass(ActivitiesRoute::class)
            subclass(ActivityDetailsRoute::class)

            subclass(AlertsRoute::class)
            subclass(DiseaseAlertDetailsRoute::class)
            subclass(WeatherAlertDetailsRoute::class)

            subclass(AccountRoute::class)
            subclass(SecuritySettingsRoute::class)
        }
    }

private val vineShieldSavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            vineShieldSerializersModule
    }

class VineShieldNavigationState(
    val vineyardBackStack: NavBackStack<NavKey>,
    val activityBackStack: NavBackStack<NavKey>,
    val alertBackStack: NavBackStack<NavKey>,
    val accountBackStack: NavBackStack<NavKey>,
    private val selectedDestinationState:
    MutableState<TopLevelDestination>
) {

    var selectedDestination:
            TopLevelDestination
            by selectedDestinationState
        private set

    val currentBackStack: NavBackStack<NavKey>
        get() =
            when (selectedDestination) {

                TopLevelDestination.VINEYARDS ->
                    vineyardBackStack

                TopLevelDestination.ACTIVITIES ->
                    activityBackStack

                TopLevelDestination.ALERTS ->
                    alertBackStack

                TopLevelDestination.ACCOUNT ->
                    accountBackStack
            }

    fun selectDestination(
        destination: TopLevelDestination
    ) {
        selectedDestination = destination
    }

    fun navigate(
        route: AppRoute
    ) {
        currentBackStack.add(route)
    }

    fun goBack(): Boolean {

        if (currentBackStack.size <= 1) {
            return false
        }

        currentBackStack.removeLastOrNull()

        return true
    }
}

@Composable
fun rememberVineShieldNavigationState():
        VineShieldNavigationState {

    val vineyardBackStack =
        rememberNavBackStack(
            configuration =
                vineShieldSavedStateConfiguration,
            VineyardsRoute
        )

    val activityBackStack =
        rememberNavBackStack(
            configuration =
                vineShieldSavedStateConfiguration,
            ActivitiesRoute
        )

    val alertBackStack =
        rememberNavBackStack(
            configuration =
                vineShieldSavedStateConfiguration,
            AlertsRoute
        )

    val accountBackStack =
        rememberNavBackStack(
            configuration =
                vineShieldSavedStateConfiguration,
            AccountRoute
        )

    val selectedDestination =
        rememberSaveable {
            mutableStateOf(
                TopLevelDestination.VINEYARDS
            )
        }

    return VineShieldNavigationState(
        vineyardBackStack =
            vineyardBackStack,
        activityBackStack =
            activityBackStack,
        alertBackStack =
            alertBackStack,
        accountBackStack =
            accountBackStack,
        selectedDestinationState =
            selectedDestination
    )
}