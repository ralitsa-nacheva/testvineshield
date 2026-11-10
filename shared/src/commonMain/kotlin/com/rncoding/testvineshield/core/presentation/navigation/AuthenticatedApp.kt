package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import com.rncoding.testvineshield.core.presentation.harvest.*
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.presentation.activities.*
import com.rncoding.testvineshield.core.domain.usecases.activity.ActivityScope
import com.rncoding.testvineshield.core.presentation.blocks.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rncoding.testvineshield.core.presentation.account.AccountScreenRoot
import com.rncoding.testvineshield.core.presentation.security.SecuritySettingsScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_list.VineyardListScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorMode
import com.rncoding.testvineshield.core.presentation.vineyard_editor.VineyardEditorScreenRoot
import com.rncoding.testvineshield.core.presentation.vineyard_details.VineyardDetailsScreenRoot
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
fun AuthenticatedApp(
    userId: Long,
    alertBadgeCount: Int = 0,
    activityBadgeCount: Int = 0
) {

    var busyRecordRoute by remember { mutableStateOf<AppRoute?>(null) }
    val navigationState =
        rememberVineShieldNavigationState()

    val availableDestinations = listOf(TopLevelDestination.VINEYARDS, TopLevelDestination.ACTIVITIES, TopLevelDestination.ACCOUNT)
    LaunchedEffect(navigationState.selectedDestination) {
        if (navigationState.selectedDestination !in availableDestinations)
            navigationState.selectDestination(TopLevelDestination.VINEYARDS)
    }

    Scaffold(
        bottomBar = {

            VineShieldNavigationBar(
                selectedDestination =
                    navigationState.selectedDestination,
                alertBadgeCount =
                    alertBadgeCount,
                activityBadgeCount =
                    activityBadgeCount,
                onDestinationSelected =
                    { if(busyRecordRoute == null) navigationState.selectDestination(it) },
                destinations = availableDestinations
            )
        }
    ) { innerPadding ->

        NavDisplay(
            backStack =
                navigationState.currentBackStack,

            onBack = {
                if(busyRecordRoute == null) navigationState.goBack()
            },

            modifier =
                Modifier.padding(
                    innerPadding
                ),

            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),

            entryProvider =
                entryProvider {

                    entry<VineyardsRoute> {

                        VineyardListScreenRoot(
                            onVineyardClick = { vineyardId ->

                                navigationState.navigate(
                                    VineyardDetailsRoute(
                                        vineyardId =
                                            vineyardId
                                    )
                                )
                            },
                            onCreateVineyard = {

                                navigationState.navigate(
                                    CreateVineyardRoute
                                )
                            }
                        )
                    }

                    entry<VineyardDetailsRoute> { route ->
                        VineyardDetailsScreenRoot(
                            vineyardId =
                                route.vineyardId,

                            onBack = {
                                navigationState.goBack()
                            },

                            onEdit = {
                                navigationState.navigate(
                                    EditVineyardRoute(
                                        vineyardId =
                                            route.vineyardId
                                    )
                                )
                            },

                            onDeleted = {
                                navigationState.goBack()
                            },
                            onBlocks = { navigationState.navigate(BlocksRoute(route.vineyardId)) },
                            onActivities = { navigationState.navigate(ActivityListRoute(route.vineyardId)) }
                        )
                    }

                    entry<CreateVineyardRoute> {
                        VineyardEditorScreenRoot(
                            mode = VineyardEditorMode.Create,
                            onBack = {
                                navigationState.goBack()
                            },
                            onSaved = {
                                navigationState.goBack()
                            }
                        )
                    }

                    entry<EditVineyardRoute> { route ->
                        VineyardEditorScreenRoot(
                            mode =
                                VineyardEditorMode.Edit(
                                    vineyardId = route.vineyardId
                                ),
                            onBack = {
                                navigationState.goBack()
                            },
                            onSaved = {
                                navigationState.goBack()
                            }
                        )
                    }

                    entry<BlocksRoute> { route ->
                        BlockListScreenRoot(route.vineyardId,userId,onBack={navigationState.goBack()},
                            onCreate={navigationState.navigate(BlockEditorRoute(route.vineyardId))},
                            onOpen={navigationState.navigate(BlockDetailsRoute(route.vineyardId,it))})
                    }
                    entry<BlockDetailsRoute> { route ->
                        BlockDetailsScreenRoot(route.vineyardId,route.blockId,userId,onBack={navigationState.goBack()},
                            onEdit={navigationState.navigate(BlockEditorRoute(route.vineyardId,route.blockId))},
                            onDeleted={navigationState.goBack()},
                            onActivities={navigationState.navigate(ActivityListRoute(route.vineyardId,route.blockId))},
                            onHarvests={navigationState.navigate(HarvestListRoute(route.vineyardId,route.blockId))},
                            onBusyChanged={ if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null })
                    }
                    entry<BlockEditorRoute> { route ->
                        BlockEditorScreenRoot(route.vineyardId,route.blockId,userId,onBack={navigationState.goBack()},
                            onSaved={navigationState.goBack()},onBusyChanged={ if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null })
                    }

                    entry<HarvestListRoute> { route ->
                        HarvestListScreenRoot(userId, HarvestScope(route.vineyardId, route.blockId),
                            onBack = { navigationState.goBack() },
                            onCreate = { navigationState.navigate(HarvestEditorRoute(route.vineyardId, route.blockId)) },
                            onOpen = { navigationState.navigate(HarvestDetailsRoute(route.vineyardId, route.blockId, it)) })
                    }
                    entry<HarvestDetailsRoute> { route ->
                        HarvestDetailsScreenRoot(userId, HarvestKey(HarvestScope(route.vineyardId, route.blockId), route.harvestId),
                            onBack = { navigationState.goBack() },
                            onEdit = { navigationState.navigate(HarvestEditorRoute(route.vineyardId, route.blockId, route.harvestId)) },
                            onDeleted = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }
                    entry<HarvestEditorRoute> { route ->
                        HarvestEditorScreenRoot(userId, HarvestKey(HarvestScope(route.vineyardId, route.blockId), route.harvestId),
                            onBack = { navigationState.goBack() }, onSaved = { navigationState.goBack() },
                            onBusyChanged = { if (it) busyRecordRoute = route else if (busyRecordRoute == route) busyRecordRoute = null })
                    }

                    entry<ActivitiesRoute> {
                        ActivityListScreenRoot(userId,onVineyard={navigationState.navigate(ActivityListRoute(it))})
                    }
                    entry<ActivityListRoute> { route ->
                        ActivityListScreenRoot(userId,ActivityScope(route.vineyardId,route.blockId),onBack={navigationState.goBack()},
                            onCreate={navigationState.navigate(ActivityEditorRoute(route.vineyardId,route.blockId))},
                            onOpen={navigationState.navigate(ActivityDetailsRoute(route.vineyardId,it))})
                    }
                    entry<ActivityDetailsRoute> { route ->
                        ActivityDetailsScreenRoot(userId,route.vineyardId,route.activityId,onBack={navigationState.goBack()},
                            onEdit={navigationState.navigate(ActivityEditorRoute(route.vineyardId,activityId=route.activityId))},
                            onComplete={navigationState.navigate(ActivityEditorRoute(route.vineyardId,activityId=route.activityId,complete=true))},
                            onDeleted={navigationState.goBack()},
                            onBusyChanged={if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null})
                    }
                    entry<ActivityEditorRoute> { route ->
                        ActivityEditorScreenRoot(userId,ActivityScope(route.vineyardId,route.blockId),route.activityId,route.complete,
                            onBack={navigationState.goBack()},onSaved={navigationState.goBack()},
                            onBusyChanged={if(it) busyRecordRoute=route else if(busyRecordRoute==route) busyRecordRoute=null})
                    }

                    entry<AlertsRoute> {

                        FeaturePlaceholderScreen(
                            title = "Alerts"
                        )
                    }

                    entry<DiseaseAlertDetailsRoute> { route ->

                        FeaturePlaceholderScreen(
                            title =
                                "Disease alert ${route.alertId}"
                        )
                    }

                    entry<WeatherAlertDetailsRoute> { route ->

                        FeaturePlaceholderScreen(
                            title =
                                "Weather alert ${route.alertId}"
                        )
                    }

                    entry<AccountRoute> {

                        AccountScreenRoot(
                            onBack = {
                                // Account is a top-level screen.
                            },
                            onSecuritySettings = {

                                navigationState.navigate(
                                    SecuritySettingsRoute
                                )
                            }
                        )
                    }

                    entry<SecuritySettingsRoute> {

                        SecuritySettingsScreenRoot(
                            userId = userId,
                            onBack = {
                                navigationState.goBack()
                            }
                        )
                    }
                }
        )
    }
}