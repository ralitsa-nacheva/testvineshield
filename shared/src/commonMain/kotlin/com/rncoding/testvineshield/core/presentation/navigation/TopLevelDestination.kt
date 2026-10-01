package com.rncoding.testvineshield.core.presentation.navigation

enum class TopLevelDestination(
    val label: String,
    val route: AppRoute
) {

    VINEYARDS(
        label = "Vineyards",
        route = VineyardsRoute
    ),

    ACTIVITIES(
        label = "Activities",
        route = ActivitiesRoute
    ),

    ALERTS(
        label = "Alerts",
        route = AlertsRoute
    ),

    ACCOUNT(
        label = "Account",
        route = AccountRoute
    )
}